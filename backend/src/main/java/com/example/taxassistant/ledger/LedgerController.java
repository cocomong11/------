package com.example.taxassistant.ledger;

import com.example.taxassistant.domain.ledger.LedgerEntry;
import com.example.taxassistant.ledger.dto.LedgerResponse;
import com.example.taxassistant.security.UserPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses/{businessId}")
public class LedgerController {

    private final LedgerService ledgerService;
    private final LedgerExcelExporter excelExporter;

    public LedgerController(LedgerService ledgerService, LedgerExcelExporter excelExporter) {
        this.ledgerService = ledgerService;
        this.excelExporter = excelExporter;
    }

    @GetMapping("/ledger")
    public LedgerResponse getLedger(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID businessId,
            @RequestParam int year,
            @RequestParam(required = false) Integer month
    ) {
        return ledgerService.getLedger(principal.getId(), businessId, year, month);
    }

    @GetMapping("/exports/ledger.xlsx")
    public ResponseEntity<byte[]> exportLedger(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID businessId,
            @RequestParam int year,
            @RequestParam(required = false) Integer month
    ) {
        List<LedgerEntry> entries = ledgerService.getLedgerEntriesForExport(
                principal.getId(),
                businessId,
                year,
                month
        );
        byte[] body = excelExporter.export(entries, year, month);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename(year, month), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(body);
    }

    private String filename(int year, Integer month) {
        if (month == null) {
            return "ledger-" + year + ".xlsx";
        }
        return "ledger-" + year + "-" + String.format("%02d", month) + ".xlsx";
    }
}

