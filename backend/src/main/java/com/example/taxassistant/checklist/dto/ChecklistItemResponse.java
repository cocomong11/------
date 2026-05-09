package com.example.taxassistant.checklist.dto;

import com.example.taxassistant.domain.checklist.ChecklistItem;
import com.example.taxassistant.domain.enums.ChecklistItemStatus;
import com.example.taxassistant.domain.enums.ChecklistItemType;
import com.example.taxassistant.domain.enums.Severity;
import java.util.UUID;

public record ChecklistItemResponse(
        UUID id,
        UUID transactionId,
        ChecklistItemType itemType,
        ChecklistItemStatus status,
        Severity severity,
        String title,
        String description
) {

    public static ChecklistItemResponse from(ChecklistItem item) {
        return new ChecklistItemResponse(
                item.getId(),
                item.getTransaction() == null ? null : item.getTransaction().getId(),
                item.getItemType(),
                item.getStatus(),
                item.getSeverity(),
                item.getTitle(),
                item.getDescription()
        );
    }
}

