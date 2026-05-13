package com.example.taxassistant.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecurityStartupValidatorTest {

    @Test
    void rejectsUnsafeProductionSettings() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(
                environment,
                "local-development-secret-key-must-be-at-least-32-bytes",
                true,
                "jdbc:h2:file:./prod"
        );

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret");
    }

    @Test
    void acceptsStrongProductionSettings() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(
                environment,
                "production-secret-value-that-is-long-random-and-loaded-from-a-secret-manager",
                false,
                "jdbc:postgresql://db.example:5432/tax_assistant"
        );

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void skipsValidationOutsideProductionProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(
                environment,
                "local-development-secret-key-must-be-at-least-32-bytes",
                true,
                "jdbc:h2:file:./local"
        );

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }
}
