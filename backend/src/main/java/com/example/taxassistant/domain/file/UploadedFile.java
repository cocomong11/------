package com.example.taxassistant.domain.file;

import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.common.BaseEntity;
import com.example.taxassistant.domain.enums.FileProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @NotBlank
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @NotBlank
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @PositiveOrZero
    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private FileProcessingStatus processingStatus = FileProcessingStatus.UPLOADED;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    protected UploadedFile() {
    }

    public UploadedFile(
            Business business,
            String originalFilename,
            String storageKey,
            String contentType,
            long fileSizeBytes
    ) {
        this.business = business;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
    }

    public void updateChecksum(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public void markParsing() {
        this.processingStatus = FileProcessingStatus.PARSING;
        this.errorMessage = null;
    }

    public void markParsed() {
        this.processingStatus = FileProcessingStatus.PARSED;
        this.errorMessage = null;
    }

    public void markFailed(String safeErrorMessage) {
        this.processingStatus = FileProcessingStatus.FAILED;
        this.errorMessage = safeErrorMessage;
    }

    public UUID getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public FileProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
