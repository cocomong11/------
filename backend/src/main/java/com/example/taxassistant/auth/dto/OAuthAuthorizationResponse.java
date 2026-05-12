package com.example.taxassistant.auth.dto;

public record OAuthAuthorizationResponse(
        String provider,
        boolean configured,
        String authorizationUrl,
        String message
) {
}
