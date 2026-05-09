package com.example.taxassistant.reports.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportSummaryResponse(
        int year,
        Integer month,
        BigDecimal totalRevenue,
        BigDecimal totalExpense,
        BigDecimal expectedIncome,
        List<CategoryExpenseResponse> categoryExpenses,
        long unclassifiedTransactionCount,
        long missingEvidenceTransactionCount,
        List<SuspiciousItemResponse> suspiciousItems,
        String notice
) {
}

