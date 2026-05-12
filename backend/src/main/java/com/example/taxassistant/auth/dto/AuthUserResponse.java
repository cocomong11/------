package com.example.taxassistant.auth.dto;

import com.example.taxassistant.domain.user.User;
import java.time.Instant;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String name,
        String status,
        boolean emailVerified,
        boolean requiredAgreementsAccepted,
        Instant lastLoginAt
) {

    public static AuthUserResponse from(User user) {
        return from(user, false);
    }

    public static AuthUserResponse from(User user, boolean requiredAgreementsAccepted) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getStatus().name(),
                user.getEmailVerifiedAt() != null,
                requiredAgreementsAccepted,
                user.getLastLoginAt()
        );
    }
}
