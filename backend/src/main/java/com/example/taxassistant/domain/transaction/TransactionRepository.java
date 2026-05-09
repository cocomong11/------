package com.example.taxassistant.domain.transaction;

import com.example.taxassistant.domain.enums.ClassificationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByBusinessId(UUID businessId);

    List<Transaction> findAllByBusinessIdAndBusinessOwnerId(UUID businessId, UUID ownerId);

    List<Transaction> findAllByBusinessIdOrderByTransactionDateDescCreatedAtDesc(UUID businessId);

    List<Transaction> findAllByBusinessIdAndBusinessOwnerIdOrderByTransactionDateDescCreatedAtDesc(
            UUID businessId,
            UUID ownerId
    );

    List<Transaction> findAllByBusinessIdAndTransactionDateBetweenOrderByTransactionDateAscCreatedAtAsc(
            UUID businessId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Transaction> findAllByBusinessIdAndBusinessOwnerIdAndTransactionDateBetweenOrderByTransactionDateAscCreatedAtAsc(
            UUID businessId,
            UUID ownerId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Transaction> findByIdAndBusinessOwnerId(UUID id, UUID ownerId);

    List<Transaction> findAllByUploadedFileId(UUID uploadedFileId);

    long countByBusinessIdAndClassificationStatus(UUID businessId, ClassificationStatus status);
}
