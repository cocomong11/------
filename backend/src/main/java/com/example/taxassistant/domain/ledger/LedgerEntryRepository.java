package com.example.taxassistant.domain.ledger;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findAllByBusinessId(UUID businessId);

    List<LedgerEntry> findAllByBusinessIdAndBusinessOwnerId(UUID businessId, UUID ownerId);

    List<LedgerEntry> findAllByBusinessIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
            UUID businessId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<LedgerEntry> findAllByBusinessIdAndBusinessOwnerIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
            UUID businessId,
            UUID ownerId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<LedgerEntry> findByTransactionId(UUID transactionId);

    Optional<LedgerEntry> findByTransactionIdAndBusinessOwnerId(UUID transactionId, UUID ownerId);
}
