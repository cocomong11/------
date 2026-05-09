package com.example.taxassistant.auth.dto;

import com.example.taxassistant.domain.user.User;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String name
) {

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getName());
    }
}

