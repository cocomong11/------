package com.example.taxassistant.health;

import java.time.Instant;

public record HealthResponse(
        String status,
        Instant checkedAt,
        String notice
) {
}

