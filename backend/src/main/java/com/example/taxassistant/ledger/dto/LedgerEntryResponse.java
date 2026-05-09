package com.example.taxassistant.ledger.dto;

import com.example.taxassistant.domain.enums.EvidenceStatus;
import com.example.taxassistant.domain.ledger.LedgerEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID transactionId,
        LocalDate entryDate,
        String accountTitle,
        String summary,
        BigDecimal revenueAmount,
        BigDecimal expenseAmount,
        EvidenceStatus evidenceStatus
) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransaction() == null ? null : entry.getTransaction().getId(),
                entry.getEntryDate(),
                entry.getAccountTitle(),
                entry.getSummary(),
                entry.getRevenueAmount(),
                entry.getExpenseAmount(),
                entry.getEvidenceStatus()
        );
    }
}

