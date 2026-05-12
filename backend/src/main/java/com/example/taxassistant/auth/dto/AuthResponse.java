package com.example.taxassistant.auth.dto;

public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        AuthUserResponse user
) {

    public static AuthResponse bearer(String accessToken, String refreshToken, AuthUserResponse user) {
        return new AuthResponse("Bearer", accessToken, refreshToken, user);
    }

    public static AuthResponse pendingVerification(AuthUserResponse user) {
        return new AuthResponse("Bearer", null, null, user);
    }
}
