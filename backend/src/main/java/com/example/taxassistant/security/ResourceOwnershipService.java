package com.example.taxassistant.security;

import com.example.taxassistant.common.error.BusinessException;
import com.example.taxassistant.common.error.ErrorCode;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.business.BusinessRepository;
import com.example.taxassistant.domain.file.UploadedFile;
import com.example.taxassistant.domain.file.UploadedFileRepository;
import com.example.taxassistant.domain.ledger.LedgerEntry;
import com.example.taxassistant.domain.ledger.LedgerEntryRepository;
import com.example.taxassistant.domain.report.TaxReport;
import com.example.taxassistant.domain.report.TaxReportRepository;
import com.example.taxassistant.domain.transaction.Transaction;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceOwnershipService {

    private final BusinessRepository businessRepository;
    private final TransactionRepository transactionRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TaxReportRepository taxReportRepository;

    public ResourceOwnershipService(
            BusinessRepository businessRepository,
            TransactionRepository transactionRepository,
            UploadedFileRepository uploadedFileRepository,
            LedgerEntryRepository ledgerEntryRepository,
            TaxReportRepository taxReportRepository
    ) {
        this.businessRepository = businessRepository;
        this.transactionRepository = transactionRepository;
        this.uploadedFileRepository = uploadedFileRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.taxReportRepository = taxReportRepository;
    }

    @Transactional(readOnly = true)
    public Business requireOwnedBusiness(UUID userId, UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(userId, business.getOwner().getId());
        return business;
    }

    @Transactional(readOnly = true)
    public Transaction requireOwnedTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(userId, transaction.getBusiness().getOwner().getId());
        return transaction;
    }

    @Transactional(readOnly = true)
    public UploadedFile requireOwnedUploadedFile(UUID userId, UUID uploadedFileId) {
        UploadedFile uploadedFile = uploadedFileRepository.findById(uploadedFileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(userId, uploadedFile.getBusiness().getOwner().getId());
        return uploadedFile;
    }

    @Transactional(readOnly = true)
    public LedgerEntry requireOwnedLedgerEntry(UUID userId, UUID ledgerEntryId) {
        LedgerEntry ledgerEntry = ledgerEntryRepository.findById(ledgerEntryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(userId, ledgerEntry.getBusiness().getOwner().getId());
        return ledgerEntry;
    }

    @Transactional(readOnly = true)
    public TaxReport requireOwnedTaxReport(UUID userId, UUID taxReportId) {
        TaxReport taxReport = taxReportRepository.findById(taxReportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(userId, taxReport.getBusiness().getOwner().getId());
        return taxReport;
    }

    private void requireOwner(UUID userId, UUID ownerId) {
        if (!ownerId.equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
