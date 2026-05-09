package com.example.taxassistant.files.parser;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedTransactionRow(
        int rowNumber,
        LocalDate transactionDate,
        String merchantName,
        String description,
        BigDecimal amount,
        BigDecimal vatAmount,
        boolean income,
        String rawData
) {
}

