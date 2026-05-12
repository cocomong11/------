package com.example.taxassistant.domain.agreement;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgreementRepository extends JpaRepository<Agreement, UUID> {

    boolean existsByUserId(UUID userId);
}
