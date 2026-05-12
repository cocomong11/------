package com.example.taxassistant.business.dto;

import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.enums.BusinessVerificationStatus;
import com.example.taxassistant.domain.enums.BusinessIndustryGroup;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BusinessResponse(
        UUID id,
        String name,
        String businessRegistrationNumber,
        String representativeName,
        String industryName,
        String taxationType,
        BusinessIndustryGroup industryGroup,
        boolean professionalBusiness,
        boolean hasEmployees,
        LocalDate openedOn,
        BigDecimal previousYearRevenue,
        BusinessVerificationStatus verificationStatus,
        BookkeepingPredictionResponse bookkeepingPrediction
) {

    public static BusinessResponse from(Business business, BookkeepingPredictionResponse prediction) {
        return new BusinessResponse(
                business.getId(),
                business.getName(),
                maskBusinessRegistrationNumber(business.getBusinessRegistrationNumber()),
                business.getRepresentativeName(),
                business.getIndustryName(),
                business.getTaxationType(),
                business.getIndustryGroup(),
                business.isProfessionalBusiness(),
                business.hasEmployees(),
                business.getOpenedOn(),
                business.getPreviousYearRevenue(),
                business.getVerificationStatus(),
                prediction
        );
    }

    private static String maskBusinessRegistrationNumber(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 10) {
            return "***-**-" + value.substring(Math.max(0, value.length() - 2));
        }
        return digits.substring(0, 3) + "-**-" + digits.substring(5, 7) + "***";
    }
}
