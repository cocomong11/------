package com.example.taxassistant.business;

import com.example.taxassistant.business.dto.BusinessVerificationRequest;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.enums.BusinessVerificationStatus;

public interface BusinessVerificationProvider {

    VerificationResult verify(Business business, BusinessVerificationRequest request);

    record VerificationResult(
            BusinessVerificationStatus status,
            String title,
            String message
    ) {
    }
}
