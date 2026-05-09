package com.example.taxassistant.files.dto;

public record FileParseErrorResponse(
        int rowNumber,
        String message,
        String rawData
) {
}

