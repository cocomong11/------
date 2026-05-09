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

        @Size(max = 150)
        String industryName,

        @NotNull
        BusinessIndustryGroup industryGroup,

        boolean professionalBusiness,

        LocalDate openedOn,

        @DecimalMin(value = "0.00")
        BigDecimal previousYearRevenue
) {
}

