package com.example.taxassistant.files.parser;

public record ParseFailure(
        int rowNumber,
        String message,
        String rawData
) {
}

