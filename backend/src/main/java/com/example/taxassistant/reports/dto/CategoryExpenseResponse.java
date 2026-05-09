package com.example.taxassistant.reports.dto;

import java.math.BigDecimal;

public record CategoryExpenseResponse(
        String categoryName,
        BigDecimal amount
) {
}

