package com.example.taxassistant.files.dto;

import com.example.taxassistant.domain.enums.FileProcessingStatus;
import com.example.taxassistant.domain.file.UploadedFile;
import java.time.Instant;
import java.util.UUID;

public record UploadedFileResponse(
        UUID id,
        String originalFilename,
        FileProcessingStatus processingStatus,
        long fileSizeBytes,
        Instant uploadedAt
) {

    public static UploadedFileResponse from(UploadedFile uploadedFile) {
        return new UploadedFileResponse(
                uploadedFile.getId(),
                uploadedFile.getOriginalFilename(),
                uploadedFile.getProcessingStatus(),
                uploadedFile.getFileSizeBytes(),
                uploadedFile.getCreatedAt()
        );
    }
}
