package com.example.taxassistant.business.dto;

import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.enums.BusinessIndustryGroup;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BusinessResponse(
        UUID id,
        String name,
        String businessRegistrationNumber,
        String industryName,
        BusinessIndustryGroup industryGroup,
        boolean professionalBusiness,
        LocalDate openedOn,
        BigDecimal previousYearRevenue,
        BookkeepingPredictionResponse bookkeepingPrediction
) {

    public static BusinessResponse from(Business business, BookkeepingPredictionResponse prediction) {
        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getBusinessRegistrationNumber(),
                business.getIndustryName(),
                business.getIndustryGroup(),
                business.isProfessionalBusiness(),
                business.getOpenedOn(),
                business.getPreviousYearRevenue(),
                prediction
        );
    }
}

