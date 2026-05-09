package com.example.taxassistant.ledger.dto;

import java.util.List;
import java.util.UUID;

public record LedgerResponse(
        UUID businessId,
        int year,
        Integer month,
        LedgerSummaryResponse summary,
        List<LedgerEntryResponse> entries,
        String notice
) {
}

