package com.example.taxassistant.domain.transaction;

import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.common.BaseEntity;
import com.example.taxassistant.domain.enums.ClassificationStatus;
import com.example.taxassistant.domain.enums.EvidenceStatus;
import com.example.taxassistant.domain.enums.TransactionType;
import com.example.taxassistant.domain.file.UploadedFile;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "business_transactions")
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_file_id")
    private UploadedFile uploadedFile;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "merchant_name", length = 255)
    private String merchantName;

    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "vat_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", nullable = false, length = 30)
    private ClassificationStatus classificationStatus = ClassificationStatus.UNCLASSIFIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", nullable = false, length = 30)
    private EvidenceStatus evidenceStatus = EvidenceStatus.UNKNOWN;

    @Column(name = "source_row_number")
    private Integer sourceRowNumber;

    @Column(name = "raw_data", columnDefinition = "text")
    private String rawData;

    @Column(name = "user_memo", length = 500)
    private String userMemo;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Transaction() {
    }

    public Transaction(
            Business business,
            UploadedFile uploadedFile,
            LocalDate transactionDate,
            String merchantName,
            String description,
            BigDecimal amount,
            TransactionType transactionType
    ) {
        this.business = business;
        this.uploadedFile = uploadedFile;
        this.transactionDate = transactionDate;
        this.merchantName = merchantName;
        this.description = description;
        this.amount = amount;
        this.transactionType = transactionType;
    }

    public void applyCategory(String categoryName, ClassificationStatus classificationStatus) {
        this.categoryName = categoryName;
        this.classificationStatus = classificationStatus;
    }

    public void updateEvidenceStatus(EvidenceStatus evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    public void updateMemo(String userMemo) {
        this.userMemo = userMemo;
    }

    public void attachRawData(Integer sourceRowNumber, String rawData) {
        this.sourceRowNumber = sourceRowNumber;
        this.rawData = rawData;
    }

    public void updateVatAmount(BigDecimal vatAmount) {
        this.vatAmount = vatAmount == null ? BigDecimal.ZERO : vatAmount;
    }

    public UUID getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public ClassificationStatus getClassificationStatus() {
        return classificationStatus;
    }

    public EvidenceStatus getEvidenceStatus() {
        return evidenceStatus;
    }

    public Integer getSourceRowNumber() {
        return sourceRowNumber;
    }

    public String getRawData() {
        return rawData;
    }

    public String getUserMemo() {
        return userMemo;
    }

    public long getVersion() {
        return version;
    }
}
