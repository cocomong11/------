package com.example.taxassistant.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataSanitizerTest {

    @Test
    void masksSensitiveValuesBeforeLogging() {
        String raw = """
                email=owner@example.com Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJvd25lciJ9.fake
                사업자등록번호=123-45-67890 merchantName=쿠팡 amount=33000
                거래처=네이버페이 차이 금액: 120,000
                """;

        String sanitized = SensitiveDataSanitizer.sanitize(raw);

        assertThat(sanitized)
                .doesNotContain("owner@example.com")
                .doesNotContain("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJvd25lciJ9.fake")
                .doesNotContain("123-45-67890")
                .doesNotContain("쿠팡")
                .doesNotContain("네이버페이")
                .doesNotContain("33000")
                .doesNotContain("120,000")
                .contains("[EMAIL]")
                .contains("Bearer [TOKEN]")
                .contains("[BUSINESS_REGISTRATION_NUMBER]")
                .contains("[COUNTERPARTY]")
                .contains("[AMOUNT]");
    }
}
