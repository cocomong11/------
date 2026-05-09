package com.example.taxassistant.transactions.dto;

import jakarta.validation.constraints.Size;

public record TransactionUpdateRequest(
        @Size(max = 100)
        String categoryName,

        @Size(max = 500)
        String userMemo
) {
}

