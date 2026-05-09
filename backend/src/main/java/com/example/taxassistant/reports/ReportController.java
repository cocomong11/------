package com.example.taxassistant.reports;

import com.example.taxassistant.reports.dto.MonthlyReportsResponse;
import com.example.taxassistant.reports.dto.YearlyReportResponse;
import com.example.taxassistant.security.UserPrincipal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses/{businessId}/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly")
    public MonthlyReportsResponse monthly(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID businessId,
            @RequestParam(required = false) Integer year
    ) {
        return reportService.monthly(principal.getId(), businessId, year);
    }

    @GetMapping("/yearly")
    public YearlyReportResponse yearly(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID businessId,
            @RequestParam(required = false) Integer year
    ) {
        return reportService.yearly(principal.getId(), businessId, year);
    }
}

