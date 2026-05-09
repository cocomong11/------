package com.example.taxassistant.domain.report;

import com.example.taxassistant.domain.enums.ReportPeriodType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxReportRepository extends JpaRepository<TaxReport, UUID> {

    List<TaxReport> findAllByBusinessIdAndPeriodType(UUID businessId, ReportPeriodType periodType);

    List<TaxReport> findAllByBusinessIdAndBusinessOwnerIdAndPeriodType(
            UUID businessId,
            UUID ownerId,
            ReportPeriodType periodType
    );
}
