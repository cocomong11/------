package com.example.taxassistant.business.dto;

import com.example.taxassistant.domain.enums.BusinessVerificationStatus;
import java.util.UUID;

public record BusinessVerificationResponse(
        UUID businessId,
        BusinessVerificationStatus verificationStatus,
        String title,
        String message,
        String notice
) {
}
