package com.example.taxassistant.business.dto;

import com.example.taxassistant.domain.enums.BusinessIndustryGroup;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BusinessRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 30)
        String businessRegistrationNumber,

        @Size(max = 100)
        String representativeName,

        @Size(max = 150)
        String industryName,

        @Size(max = 50)
        String taxationType,

        @NotNull
        BusinessIndustryGroup industryGroup,

        boolean professionalBusiness,

        boolean hasEmployees,

        LocalDate openedOn,

        @DecimalMin(value = "0.00")
        BigDecimal previousYearRevenue
) {

    public BusinessRequest(
            String name,
            String businessRegistrationNumber,
            String industryName,
            BusinessIndustryGroup industryGroup,
            boolean professionalBusiness,
            LocalDate openedOn,
            BigDecimal previousYearRevenue
    ) {
        this(
                name,
                businessRegistrationNumber,
                null,
                industryName,
                null,
                industryGroup,
                professionalBusiness,
                false,
                openedOn,
                previousYearRevenue
        );
    }
}
