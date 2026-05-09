package com.example.taxassistant.domain.report;

import com.example.taxassistant.common.LegalNotice;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.common.BaseEntity;
import com.example.taxassistant.domain.enums.ReportPeriodType;
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
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tax_reports")
public class TaxReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 30)
    private ReportPeriodType periodType;

    @NotNull
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @NotNull
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_revenue", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_expense", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(name = "net_income", nullable = false, precision = 18, scale = 2)
    private BigDecimal netIncome = BigDecimal.ZERO;

    @Column(name = "unclassified_transaction_count", nullable = false)
    private int unclassifiedTransactionCount;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Column(name = "notice", nullable = false, length = 255)
    private String notice = LegalNotice.REFERENCE_ONLY;

    protected TaxReport() {
    }

    public TaxReport(
            Business business,
            ReportPeriodType periodType,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal totalRevenue,
            BigDecimal totalExpense,
            int unclassifiedTransactionCount
    ) {
        this.business = business;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalRevenue = totalRevenue;
        this.totalExpense = totalExpense;
        this.netIncome = totalRevenue.subtract(totalExpense);
        this.unclassifiedTransactionCount = unclassifiedTransactionCount;
    }

    public UUID getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public ReportPeriodType getPeriodType() {
        return periodType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public BigDecimal getNetIncome() {
        return netIncome;
    }

    public int getUnclassifiedTransactionCount() {
        return unclassifiedTransactionCount;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public String getNotice() {
        return notice;
    }
}

