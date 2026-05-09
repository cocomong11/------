package com.example.taxassistant.ledger;

import com.example.taxassistant.common.LegalNotice;
import com.example.taxassistant.common.error.BusinessException;
import com.example.taxassistant.common.error.ErrorCode;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.enums.EvidenceStatus;
import com.example.taxassistant.domain.enums.TransactionType;
import com.example.taxassistant.domain.ledger.LedgerEntry;
import com.example.taxassistant.domain.ledger.LedgerEntryRepository;
import com.example.taxassistant.domain.transaction.Transaction;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.ledger.dto.LedgerEntryResponse;
import com.example.taxassistant.ledger.dto.LedgerResponse;
import com.example.taxassistant.ledger.dto.LedgerSummaryResponse;
import com.example.taxassistant.security.ResourceOwnershipService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ResourceOwnershipService ownershipService;

    public LedgerService(
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            ResourceOwnershipService ownershipService
    ) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ownershipService = ownershipService;
    }

    @Transactional
    public LedgerResponse getLedger(UUID userId, UUID businessId, int year, Integer month) {
        Business business = ownershipService.requireOwnedBusiness(userId, businessId);
        LedgerPeriod period = resolvePeriod(year, month);
        generateEntries(business, userId, period);

        List<LedgerEntry> entries = ledgerEntryRepository
                .findAllByBusinessIdAndBusinessOwnerIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
                        businessId,
                        userId,
                        period.startDate(),
                        period.endDate()
                );
        return toResponse(businessId, period, entries);
    }

    @Transactional
    public List<LedgerEntry> getLedgerEntriesForExport(UUID userId, UUID businessId, int year, Integer month) {
        Business business = ownershipService.requireOwnedBusiness(userId, businessId);
        LedgerPeriod period = resolvePeriod(year, month);
        generateEntries(business, userId, period);
        return ledgerEntryRepository.findAllByBusinessIdAndBusinessOwnerIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
                businessId,
                userId,
                period.startDate(),
                period.endDate()
        );
    }

    private void generateEntries(Business business, UUID userId, LedgerPeriod period) {
        List<Transaction> transactions = transactionRepository
                .findAllByBusinessIdAndBusinessOwnerIdAndTransactionDateBetweenOrderByTransactionDateAscCreatedAtAsc(
                        business.getId(),
                        userId,
                        period.startDate(),
                        period.endDate()
                );

        for (Transaction transaction : transactions) {
            LedgerEntry entry = ledgerEntryRepository.findByTransactionIdAndBusinessOwnerId(transaction.getId(), userId)
                    .orElseGet(() -> new LedgerEntry(
                            business,
                            transaction,
                            transaction.getTransactionDate(),
                            accountTitle(transaction),
                            summary(transaction),
                            revenueAmount(transaction),
                            expenseAmount(transaction)
                    ));
            entry.refreshFromTransaction(
                    transaction.getTransactionDate(),
                    accountTitle(transaction),
                    summary(transaction),
                    revenueAmount(transaction),
                    expenseAmount(transaction),
                    evidenceStatus(transaction)
            );
            ledgerEntryRepository.save(entry);
        }
    }

    private LedgerResponse toResponse(UUID businessId, LedgerPeriod period, List<LedgerEntry> entries) {
        BigDecimal totalRevenue = entries.stream()
                .map(LedgerEntry::getRevenueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = entries.stream()
                .map(LedgerEntry::getExpenseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new LedgerResponse(
                businessId,
                period.year(),
                period.month(),
                new LedgerSummaryResponse(totalRevenue, totalExpense, totalRevenue.subtract(totalExpense)),
                entries.stream().map(LedgerEntryResponse::from).toList(),
                LegalNotice.REFERENCE_ONLY
        );
    }

    private LedgerPeriod resolvePeriod(int year, Integer month) {
        try {
            return LedgerPeriod.of(year, month);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String accountTitle(Transaction transaction) {
        if (transaction.getCategoryName() == null || transaction.getCategoryName().isBlank()) {
            return "미분류";
        }
        return transaction.getCategoryName();
    }

    private String summary(Transaction transaction) {
        if (transaction.getDescription() != null && !transaction.getDescription().isBlank()) {
            return transaction.getDescription();
        }
        if (transaction.getMerchantName() != null && !transaction.getMerchantName().isBlank()) {
            return transaction.getMerchantName();
        }
        return "거래";
    }

    private BigDecimal revenueAmount(Transaction transaction) {
        return transaction.getTransactionType() == TransactionType.INCOME
                ? transaction.getAmount()
                : BigDecimal.ZERO;
    }

    private BigDecimal expenseAmount(Transaction transaction) {
        return transaction.getTransactionType() == TransactionType.EXPENSE
                ? transaction.getAmount()
                : BigDecimal.ZERO;
    }

    private EvidenceStatus evidenceStatus(Transaction transaction) {
        return transaction.getEvidenceStatus() == null ? EvidenceStatus.UNKNOWN : transaction.getEvidenceStatus();
    }
}
