package com.example.taxassistant.ledger.dto;

import java.math.BigDecimal;

public record LedgerSummaryResponse(
        BigDecimal totalRevenue,
        BigDecimal totalExpense,
        BigDecimal netIncome
) {
}

