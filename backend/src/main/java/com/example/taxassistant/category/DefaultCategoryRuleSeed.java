package com.example.taxassistant.category;

import com.example.taxassistant.domain.enums.MatchType;
import com.example.taxassistant.domain.enums.TransactionType;

public record DefaultCategoryRuleSeed(
        String keyword,
        MatchType matchType,
        String categoryName,
        TransactionType transactionType,
        boolean requiresReview
) {
}

