package com.example.taxassistant.reports;

import com.example.taxassistant.common.error.BusinessException;
import com.example.taxassistant.common.error.ErrorCode;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.enums.ReportPeriodType;
import com.example.taxassistant.domain.report.TaxReport;
import com.example.taxassistant.domain.report.TaxReportRepository;
import com.example.taxassistant.domain.transaction.Transaction;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.reports.dto.MonthlyReportsResponse;
import com.example.taxassistant.reports.dto.ReportSummaryResponse;
import com.example.taxassistant.reports.dto.YearlyReportResponse;
import com.example.taxassistant.security.ResourceOwnershipService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final TaxReportRepository taxReportRepository;
    private final ReportComputationService computationService;
    private final ResourceOwnershipService ownershipService;
    private final Clock clock;

    public ReportService(
            TransactionRepository transactionRepository,
            TaxReportRepository taxReportRepository,
            ReportComputationService computationService,
            ResourceOwnershipService ownershipService
    ) {
        this.transactionRepository = transactionRepository;
        this.taxReportRepository = taxReportRepository;
        this.computationService = computationService;
        this.ownershipService = ownershipService;
        this.clock = Clock.systemDefaultZone();
    }

    @Transactional
    public MonthlyReportsResponse monthly(UUID userId, UUID businessId, Integer requestedYear) {
        Business business = ownershipService.requireOwnedBusiness(userId, businessId);
        int year = resolveYear(requestedYear);
        List<ReportSummaryResponse> reports = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> summarizeAndSave(business, userId, year, month))
                .toList();
        return new MonthlyReportsResponse(businessId, year, reports);
    }

    @Transactional
    public YearlyReportResponse yearly(UUID userId, UUID businessId, Integer requestedYear) {
        Business business = ownershipService.requireOwnedBusiness(userId, businessId);
        int year = resolveYear(requestedYear);
        return new YearlyReportResponse(businessId, summarizeAndSave(business, userId, year, null));
    }

    private ReportSummaryResponse summarizeAndSave(Business business, UUID userId, int year, Integer month) {
        LocalDate startDate = month == null ? LocalDate.of(year, 1, 1) : LocalDate.of(year, month, 1);
        LocalDate endDate = month == null
                ? LocalDate.of(year, 12, 31)
                : startDate.withDayOfMonth(startDate.lengthOfMonth());
        List<Transaction> transactions = transactionRepository
                .findAllByBusinessIdAndBusinessOwnerIdAndTransactionDateBetweenOrderByTransactionDateAscCreatedAtAsc(
                        business.getId(),
                        userId,
                        startDate,
                        endDate
                );
        ReportSummaryResponse summary = computationService.summarize(year, month, transactions);
        taxReportRepository.save(new TaxReport(
                business,
                month == null ? ReportPeriodType.YEARLY : ReportPeriodType.MONTHLY,
                startDate,
                endDate,
                summary.totalRevenue(),
                summary.totalExpense(),
                (int) summary.unclassifiedTransactionCount()
        ));
        return summary;
    }

    private int resolveYear(Integer requestedYear) {
        int year = requestedYear == null ? LocalDate.now(clock).getYear() : requestedYear;
        if (year < 2000 || year > 2100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return year;
    }
}
