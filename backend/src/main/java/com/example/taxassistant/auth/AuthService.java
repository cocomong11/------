package com.example.taxassistant.auth;

import com.example.taxassistant.auth.dto.AuthResponse;
import com.example.taxassistant.auth.dto.AuthUserResponse;
import com.example.taxassistant.auth.dto.LoginRequest;
import com.example.taxassistant.auth.dto.SignupRequest;
import com.example.taxassistant.common.error.BusinessException;
import com.example.taxassistant.common.error.ErrorCode;
import com.example.taxassistant.domain.user.User;
import com.example.taxassistant.domain.user.UserRepository;
import com.example.taxassistant.security.JwtTokenProvider;
import com.example.taxassistant.security.UserPrincipal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = userRepository.save(new User(
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim()
        ));
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return AuthUserResponse.from(user);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtTokenProvider.createToken(UserPrincipal.from(user));
        return AuthResponse.bearer(token, AuthUserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

