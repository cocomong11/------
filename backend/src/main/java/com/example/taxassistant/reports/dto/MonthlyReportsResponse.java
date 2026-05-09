package com.example.taxassistant.reports.dto;

import java.util.List;
import java.util.UUID;

public record MonthlyReportsResponse(
        UUID businessId,
        int year,
        List<ReportSummaryResponse> reports
) {
}

