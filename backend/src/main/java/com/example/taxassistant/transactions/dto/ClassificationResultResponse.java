package com.example.taxassistant.transactions.dto;

import java.util.List;

public record ClassificationResultResponse(
        int totalCount,
        int autoClassifiedCount,
        int needsReviewCount,
        List<TransactionResponse> transactions
) {
}

