package com.example.taxassistant.domain.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    Optional<EmailVerificationCode> findTopByUserEmailAndUsedAtIsNullOrderByCreatedAtDesc(String email);
}
