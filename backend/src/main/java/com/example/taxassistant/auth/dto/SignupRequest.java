package com.example.taxassistant.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Email
        @NotBlank
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        boolean termsAgreed,

        boolean privacyAgreed,

        boolean businessInfoConsentAgreed,

        boolean taxDataConsentAgreed,

        boolean referenceNoticeAgreed
) {

    public SignupRequest(String name, String email, String password) {
        this(name, email, password, true, true, true, true, true);
    }
}
