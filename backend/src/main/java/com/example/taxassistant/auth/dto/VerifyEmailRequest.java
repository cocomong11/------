package com.example.taxassistant.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @Email
        @NotBlank
        String email,

        @Pattern(regexp = "\\d{6}", message = "6자리 인증 코드를 입력해주세요.")
        String code
) {
}
