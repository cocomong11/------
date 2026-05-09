package com.example.taxassistant.domain.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    List<Business> findAllByOwnerId(UUID ownerId);

    Optional<Business> findByIdAndOwnerId(UUID id, UUID ownerId);
}
