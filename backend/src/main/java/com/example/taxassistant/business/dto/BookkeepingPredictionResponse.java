package com.example.taxassistant.business.dto;

import com.example.taxassistant.domain.enums.BookkeepingType;

public record BookkeepingPredictionResponse(
        BookkeepingType bookkeepingType,
        String title,
        String message,
        String notice
) {
}

