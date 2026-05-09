package com.example.taxassistant.reports.dto;

import java.math.BigDecimal;

public record SuspiciousItemResponse(
        String title,
        String description,
        BigDecimal differenceAmount
) {
}

