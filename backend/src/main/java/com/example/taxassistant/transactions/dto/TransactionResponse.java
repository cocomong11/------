package com.example.taxassistant.transactions.dto;

import com.example.taxassistant.domain.enums.ClassificationStatus;
import com.example.taxassistant.domain.enums.EvidenceStatus;
import com.example.taxassistant.domain.enums.TransactionType;
import com.example.taxassistant.domain.transaction.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID businessId,
        UUID uploadedFileId,
        LocalDate transactionDate,
        String merchantName,
        String description,
        BigDecimal amount,
        BigDecimal vatAmount,
        TransactionType transactionType,
        String categoryName,
        ClassificationStatus classificationStatus,
        EvidenceStatus evidenceStatus,
        String userMemo
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getBusiness().getId(),
                transaction.getUploadedFile() == null ? null : transaction.getUploadedFile().getId(),
                transaction.getTransactionDate(),
                transaction.getMerchantName(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getVatAmount(),
                transaction.getTransactionType(),
                transaction.getCategoryName(),
                transaction.getClassificationStatus(),
                transaction.getEvidenceStatus(),
                transaction.getUserMemo()
        );
    }
}

