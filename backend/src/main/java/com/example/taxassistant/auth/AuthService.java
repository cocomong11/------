package com.example.taxassistant.auth;

import com.example.taxassistant.auth.dto.AuthResponse;
import com.example.taxassistant.auth.dto.AuthUserResponse;
import com.example.taxassistant.auth.dto.EmailRequest;
import com.example.taxassistant.auth.dto.LoginRequest;
import com.example.taxassistant.auth.dto.MessageResponse;
import com.example.taxassistant.auth.dto.RefreshTokenRequest;
import com.example.taxassistant.auth.dto.ResetPasswordRequest;
import com.example.taxassistant.auth.dto.SignupRequest;
import com.example.taxassistant.auth.dto.VerifyEmailRequest;
import com.example.taxassistant.common.error.BusinessException;
import com.example.taxassistant.common.error.ErrorCode;
import com.example.taxassistant.domain.agreement.Agreement;
import com.example.taxassistant.domain.agreement.AgreementRepository;
import com.example.taxassistant.domain.auth.EmailVerificationCode;
import com.example.taxassistant.domain.auth.EmailVerificationCodeRepository;
import com.example.taxassistant.domain.auth.PasswordResetToken;
import com.example.taxassistant.domain.auth.PasswordResetTokenRepository;
import com.example.taxassistant.domain.auth.RefreshToken;
import com.example.taxassistant.domain.auth.RefreshTokenRepository;
import com.example.taxassistant.domain.enums.UserStatus;
import com.example.taxassistant.domain.user.User;
import com.example.taxassistant.domain.user.UserRepository;
import com.example.taxassistant.security.JwtTokenProvider;
import com.example.taxassistant.security.UserPrincipal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String CURRENT_TERMS_VERSION = "2026-05-09";
    private static final String CURRENT_PRIVACY_VERSION = "2026-05-09";
    private static final String CURRENT_BUSINESS_INFO_CONSENT_VERSION = "2026-05-09";
    private static final String CURRENT_TAX_DATA_CONSENT_VERSION = "2026-05-09";
    private static final String CURRENT_REFERENCE_NOTICE_VERSION = "2026-05-09";

    private final UserRepository userRepository;
    private final AgreementRepository agreementRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final SecureTokenService secureTokenService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final boolean autoVerifyEmail;
    private final String fixedVerificationCode;
    private final long verificationCodeExpirationMinutes;
    private final long refreshTokenExpirationDays;
    private final long passwordResetExpirationMinutes;
    private final long accountLockMinutes;

    public AuthService(
            UserRepository userRepository,
            AgreementRepository agreementRepository,
            EmailVerificationCodeRepository emailVerificationCodeRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            EmailService emailService,
            SecureTokenService secureTokenService,
            @Value("${app.security.email.auto-verify:false}") boolean autoVerifyEmail,
            @Value("${app.security.email.fixed-code:}") String fixedVerificationCode,
            @Value("${app.security.email.verification-expiration-minutes:10}") long verificationCodeExpirationMinutes,
            @Value("${app.security.refresh-token.expiration-days:14}") long refreshTokenExpirationDays,
            @Value("${app.security.password-reset.expiration-minutes:30}") long passwordResetExpirationMinutes,
            @Value("${app.security.login-lock-minutes:15}") long accountLockMinutes
    ) {
        this.userRepository = userRepository;
        this.agreementRepository = agreementRepository;
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.secureTokenService = secureTokenService;
        this.autoVerifyEmail = autoVerifyEmail;
        this.fixedVerificationCode = fixedVerificationCode;
        this.verificationCodeExpirationMinutes = verificationCodeExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.passwordResetExpirationMinutes = passwordResetExpirationMinutes;
        this.accountLockMinutes = accountLockMinutes;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request, String ipAddress) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = userRepository.save(new User(
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim()
        ));
        if (hasAllRequiredAgreements(request)) {
            saveAgreement(user, ipAddress);
        }

        if (autoVerifyEmail) {
            user.markEmailVerified();
            return issueToken(user, ipAddress);
        }

        createEmailVerificationCode(user);
        return AuthResponse.pendingVerification(AuthUserResponse.from(user, agreementRepository.existsByUserId(user.getId())));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        Instant now = Instant.now();
        if (user.isLocked(now)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getLockedUntil() != null && !user.isLocked(now)) {
            user.resetLoginFailures();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordLoginFailure(now.plusSeconds(accountLockMinutes * 60));
            throw new BusinessException(user.isLocked(now.plusSeconds(1))
                    ? ErrorCode.ACCOUNT_LOCKED
                    : ErrorCode.INVALID_CREDENTIALS);
        }
        requireActiveUser(user);
        user.recordLoginSuccess();
        return issueToken(user, ipAddress);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return AuthUserResponse.from(user, agreementRepository.existsByUserId(userId));
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request, String ipAddress) {
        String email = normalizeEmail(request.email());
        EmailVerificationCode verificationCode = emailVerificationCodeRepository
                .findTopByUserEmailAndUsedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE));
        if (verificationCode.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.EXPIRED_VERIFICATION_CODE);
        }
        if (!verificationCode.getCode().equals(request.code())) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        verificationCode.markUsed();
        User user = verificationCode.getUser();
        user.markEmailVerified();
        return issueToken(user, ipAddress);
    }

    @Transactional
    public MessageResponse resendVerificationCode(EmailRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (user.getStatus() == UserStatus.ACTIVE) {
            return new MessageResponse("이미 이메일 인증이 완료되었습니다.");
        }
        createEmailVerificationCode(user);
        return new MessageResponse("인증 코드를 다시 발급했습니다.");
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String ipAddress) {
        String tokenHash = secureTokenService.sha256(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        User user = refreshToken.getUser();
        if (!refreshToken.isActive(Instant.now())) {
            refreshTokenRepository.deleteAllByUserId(user.getId());
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        requireActiveUser(user);
        String newRefreshToken = secureTokenService.newOpaqueToken();
        String newRefreshTokenHash = secureTokenService.sha256(newRefreshToken);
        refreshToken.revoke(newRefreshTokenHash);
        refreshTokenRepository.save(new RefreshToken(
                user,
                newRefreshTokenHash,
                Instant.now().plusSeconds(refreshTokenExpirationDays * 24 * 60 * 60),
                ipAddress
        ));
        return AuthResponse.bearer(jwtTokenProvider.createToken(UserPrincipal.from(user)), newRefreshToken,
                AuthUserResponse.from(user, agreementRepository.existsByUserId(user.getId())));
    }

    @Transactional
    public MessageResponse logout(RefreshTokenRequest request) {
        String tokenHash = secureTokenService.sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(refreshToken -> refreshToken.revoke(null));
        return new MessageResponse("로그아웃되었습니다.");
    }

    @Transactional
    public MessageResponse forgotPassword(EmailRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = secureTokenService.newOpaqueToken();
            passwordResetTokenRepository.save(new PasswordResetToken(
                    user,
                    secureTokenService.sha256(resetToken),
                    Instant.now().plusSeconds(passwordResetExpirationMinutes * 60)
            ));
            emailService.sendPasswordResetLink(user.getEmail(), resetToken);
        });
        return new MessageResponse("가입된 이메일이라면 비밀번호 재설정 안내를 발송했습니다.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(secureTokenService.sha256(request.token()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESET_TOKEN));
        if (!resetToken.isActive(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
        }
        User user = resetToken.getUser();
        user.replacePassword(passwordEncoder.encode(request.newPassword()));
        resetToken.markUsed();
        refreshTokenRepository.deleteAllByUserId(user.getId());
        return new MessageResponse("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
    }

    public boolean hasRequiredAgreements(UUID userId) {
        return agreementRepository.existsByUserId(userId);
    }

    private AuthResponse issueToken(User user, String ipAddress) {
        String refreshToken = secureTokenService.newOpaqueToken();
        refreshTokenRepository.save(new RefreshToken(
                user,
                secureTokenService.sha256(refreshToken),
                Instant.now().plusSeconds(refreshTokenExpirationDays * 24 * 60 * 60),
                ipAddress
        ));
        return AuthResponse.bearer(
                jwtTokenProvider.createToken(UserPrincipal.from(user)),
                refreshToken,
                AuthUserResponse.from(user, agreementRepository.existsByUserId(user.getId()))
        );
    }

    private void requireActiveUser(User user) {
        if (user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void createEmailVerificationCode(User user) {
        String code = fixedVerificationCode == null || fixedVerificationCode.isBlank()
                ? String.format("%06d", secureRandom.nextInt(1_000_000))
                : fixedVerificationCode;
        emailVerificationCodeRepository.save(new EmailVerificationCode(
                user,
                code,
                Instant.now().plusSeconds(verificationCodeExpirationMinutes * 60)
        ));
        emailService.sendVerificationCode(user.getEmail(), code);
    }

    private void saveAgreement(User user, String ipAddress) {
        agreementRepository.save(new Agreement(
                user,
                CURRENT_TERMS_VERSION,
                CURRENT_PRIVACY_VERSION,
                CURRENT_BUSINESS_INFO_CONSENT_VERSION,
                CURRENT_TAX_DATA_CONSENT_VERSION,
                CURRENT_REFERENCE_NOTICE_VERSION,
                ipAddress
        ));
    }

    private boolean hasAllRequiredAgreements(SignupRequest request) {
        return request.termsAgreed()
                && request.privacyAgreed()
                && request.businessInfoConsentAgreed()
                && request.taxDataConsentAgreed()
                && request.referenceNoticeAgreed();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
