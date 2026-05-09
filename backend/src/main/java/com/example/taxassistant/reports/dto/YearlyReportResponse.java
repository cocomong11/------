package com.example.taxassistant.reports.dto;

import java.util.UUID;

public record YearlyReportResponse(
        UUID businessId,
        ReportSummaryResponse report
) {
}

