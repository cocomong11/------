package com.example.taxassistant.domain.file;

import com.example.taxassistant.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
@Table(name = "uploaded_file_parse_errors")
public class UploadedFileParseError extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_file_id", nullable = false)
    private UploadedFile uploadedFile;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @NotBlank
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "raw_data", columnDefinition = "text")
    private String rawData;

    protected UploadedFileParseError() {
    }

    public UploadedFileParseError(UploadedFile uploadedFile, int rowNumber, String message, String rawData) {
        this.uploadedFile = uploadedFile;
        this.rowNumber = rowNumber;
        this.message = message;
        this.rawData = rawData;
    }

    public UUID getId() {
        return id;
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getMessage() {
        return message;
    }

    public String getRawData() {
        return rawData;
    }
}

