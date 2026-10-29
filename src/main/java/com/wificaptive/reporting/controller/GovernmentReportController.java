package com.wificaptive.reporting.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.reporting.dto.GovernmentReportDto;
import com.wificaptive.reporting.service.GovernmentReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/v1/admin/reports/government")
@PreAuthorize("hasRole('ADMIN')")
public class GovernmentReportController {

    private final GovernmentReportService reportService;

    public GovernmentReportController(GovernmentReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<GovernmentReportDto>> getGovernmentSummary(
            @RequestParam(defaultValue = "MONTH") String period
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.generateReport(period)));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportGovernmentCsv(
            @RequestParam(defaultValue = "MONTH") String period
    ) {
        String csv = reportService.exportReportCsv(period);
        // Prepend UTF-8 BOM so Excel opens Vietnamese characters correctly
        byte[] csvBytes = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Bao_cao_UBND_So_TTTT_Hai_Phong.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }
}
