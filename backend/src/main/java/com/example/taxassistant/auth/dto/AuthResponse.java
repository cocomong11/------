package com.example.taxassistant.auth.dto;

public record AuthResponse(
        String tokenType,
        String accessToken,
        AuthUserResponse user
) {

    public static AuthResponse bearer(String accessToken, AuthUserResponse user) {
        return new AuthResponse("Bearer", accessToken, user);
    }
}

