package com.example.taxassistant.checklist.dto;

import java.util.List;
import java.util.UUID;

public record ChecklistResponse(
        UUID businessId,
        int totalCount,
        int dangerCount,
        int warningCount,
        int normalCount,
        List<ChecklistItemResponse> items,
        String notice
) {
}

