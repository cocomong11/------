package com.example.taxassistant.files.parser;

import java.util.List;

public record TransactionParseResult(
        List<ParsedTransactionRow> rows,
        List<ParseFailure> failures
) {
}

