package com.example.taxassistant.auth;

import com.example.taxassistant.auth.dto.AuthResponse;
import com.example.taxassistant.auth.dto.AuthUserResponse;
import com.example.taxassistant.auth.dto.EmailRequest;
import com.example.taxassistant.auth.dto.LoginRequest;
import com.example.taxassistant.auth.dto.MessageResponse;
import com.example.taxassistant.auth.dto.OAuthAuthorizationResponse;
import com.example.taxassistant.auth.dto.RefreshTokenRequest;
import com.example.taxassistant.auth.dto.ResetPasswordRequest;
import com.example.taxassistant.auth.dto.SignupRequest;
import com.example.taxassistant.auth.dto.VerifyEmailRequest;
import com.example.taxassistant.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthAuthorizationService oAuthAuthorizationService;

    public AuthController(AuthService authService, OAuthAuthorizationService oAuthAuthorizationService) {
        this.authService = authService;
        this.oAuthAuthorizationService = oAuthAuthorizationService;
    }

    @PostMapping("/signup")
    public AuthResponse signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest servletRequest
    ) {
        return authService.signup(request, clientIp(servletRequest));
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return authService.login(request, clientIp(servletRequest));
    }

    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest servletRequest
    ) {
        return authService.verifyEmail(request, clientIp(servletRequest));
    }

    @PostMapping("/resend-verification-code")
    public MessageResponse resendVerificationCode(@Valid @RequestBody EmailRequest request) {
        return authService.resendVerificationCode(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        return authService.refresh(request, clientIp(servletRequest));
    }

    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.logout(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.getMe(principal.getId());
    }

    @GetMapping("/oauth/{provider}/authorization-url")
    public OAuthAuthorizationResponse oauthAuthorizationUrl(
            @PathVariable String provider,
            @RequestParam String redirectUri
    ) {
        return oAuthAuthorizationService.authorizationUrl(provider, redirectUri);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
