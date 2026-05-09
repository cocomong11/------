package com.example.taxassistant.files.dto;

import com.example.taxassistant.domain.enums.FileProcessingStatus;
import java.util.List;
import java.util.UUID;

public record FileUploadResponse(
        UUID fileId,
        String originalFilename,
        FileProcessingStatus processingStatus,
        int parsedCount,
        int failedCount,
        List<FileParseErrorResponse> errors
) {
}

