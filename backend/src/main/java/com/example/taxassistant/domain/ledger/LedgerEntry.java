package com.example.taxassistant.domain.ledger;

import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.common.BaseEntity;
import com.example.taxassistant.domain.enums.EvidenceStatus;
import com.example.taxassistant.domain.transaction.Transaction;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", unique = true)
    private Transaction transaction;

    @NotNull
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "account_title", nullable = false, length = 100)
    private String accountTitle;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "revenue_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal revenueAmount = BigDecimal.ZERO;

    @Column(name = "expense_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal expenseAmount = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", nullable = false, length = 30)
    private EvidenceStatus evidenceStatus = EvidenceStatus.UNKNOWN;

    protected LedgerEntry() {
    }

    public LedgerEntry(
            Business business,
            Transaction transaction,
            LocalDate entryDate,
            String accountTitle,
            String summary,
            BigDecimal revenueAmount,
            BigDecimal expenseAmount
    ) {
        this.business = business;
        this.transaction = transaction;
        this.entryDate = entryDate;
        this.accountTitle = accountTitle;
        this.summary = summary;
        this.revenueAmount = revenueAmount;
        this.expenseAmount = expenseAmount;
    }

    public void refreshFromTransaction(
            LocalDate entryDate,
            String accountTitle,
            String summary,
            BigDecimal revenueAmount,
            BigDecimal expenseAmount,
            EvidenceStatus evidenceStatus
    ) {
        this.entryDate = entryDate;
        this.accountTitle = accountTitle;
        this.summary = summary;
        this.revenueAmount = revenueAmount;
        this.expenseAmount = expenseAmount;
        this.evidenceStatus = evidenceStatus;
    }

    public UUID getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public String getAccountTitle() {
        return accountTitle;
    }

    public String getSummary() {
        return summary;
    }

    public BigDecimal getRevenueAmount() {
        return revenueAmount;
    }

    public BigDecimal getExpenseAmount() {
        return expenseAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public EvidenceStatus getEvidenceStatus() {
        return evidenceStatus;
    }
}
