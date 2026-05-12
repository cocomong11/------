package com.example.taxassistant.domain.file;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {

    List<UploadedFile> findAllByBusinessId(UUID businessId);

    List<UploadedFile> findAllByBusinessIdAndBusinessOwnerId(UUID businessId, UUID ownerId);

    List<UploadedFile> findAllByBusinessIdAndBusinessOwnerIdOrderByCreatedAtDesc(UUID businessId, UUID ownerId);

    Optional<UploadedFile> findByIdAndBusinessOwnerId(UUID id, UUID ownerId);
}
