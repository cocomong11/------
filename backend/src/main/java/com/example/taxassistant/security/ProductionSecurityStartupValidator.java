package com.example.taxassistant.security;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecurityStartupValidator implements ApplicationRunner {

    private static final String DEVELOPMENT_JWT_SECRET = "local-development-secret-key-must-be-at-least-32-bytes";

    private final Environment environment;
    private final String jwtSecret;
    private final boolean emailAutoVerify;
    private final String datasourceUrl;

    public ProductionSecurityStartupValidator(
            Environment environment,
            @Value("${app.security.jwt.secret}") String jwtSecret,
            @Value("${app.security.email.auto-verify:false}") boolean emailAutoVerify,
            @Value("${spring.datasource.url}") String datasourceUrl
    ) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.emailAutoVerify = emailAutoVerify;
        this.datasourceUrl = datasourceUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProductionProfile()) {
            return;
        }
        if (jwtSecret == null
                || jwtSecret.length() < 48
                || DEVELOPMENT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("Production JWT secret must be a strong external secret.");
        }
        if (emailAutoVerify) {
            throw new IllegalStateException("Email auto verification must be disabled in production.");
        }
        if (datasourceUrl != null && datasourceUrl.startsWith("jdbc:h2:")) {
            throw new IllegalStateException("H2 database must not be used in production.");
        }
    }

    private boolean isProductionProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
