package com.example.taxassistant.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record BusinessVerificationRequest(
        @NotBlank
        @Size(max = 30)
        String businessRegistrationNumber,

        @NotBlank
        @Size(max = 100)
        String representativeName,

        @NotNull
        LocalDate openedOn
) {
}
