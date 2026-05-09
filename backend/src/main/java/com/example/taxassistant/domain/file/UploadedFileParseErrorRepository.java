package com.example.taxassistant.domain.file;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileParseErrorRepository extends JpaRepository<UploadedFileParseError, UUID> {

    List<UploadedFileParseError> findAllByUploadedFileIdOrderByRowNumberAsc(UUID uploadedFileId);

    long countByUploadedFileId(UUID uploadedFileId);
}

