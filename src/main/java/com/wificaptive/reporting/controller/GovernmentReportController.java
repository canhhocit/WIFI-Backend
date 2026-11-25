package com.wificaptive.reporting.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.reporting.dto.GovernmentReportDto;
import com.wificaptive.reporting.dto.GovernmentReportHistoryDto;
import com.wificaptive.reporting.dto.SubmitReportRequest;
import com.wificaptive.reporting.service.GovernmentReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/admin/reports/government", "/v1/admin/reports/government"})
@PreAuthorize("@rbac.hasPermission('SETTINGS', 'VIEW')")
public class GovernmentReportController {

    private final GovernmentReportService reportService;
    private final com.wificaptive.reporting.service.GovernmentReportPdfService pdfService;

    public GovernmentReportController(
            GovernmentReportService reportService,
            com.wificaptive.reporting.service.GovernmentReportPdfService pdfService) {
        this.reportService = reportService;
        this.pdfService = pdfService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<GovernmentReportDto>> getGovernmentSummary(
            @RequestParam(defaultValue = "MONTH") String period
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.generateReport(period)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<GovernmentReportHistoryDto>>> getReportHistory() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getReportHistory()));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<GovernmentReportHistoryDto>> submitReport(
            @Valid @RequestBody SubmitReportRequest req,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "sotttt@haiphong.gov.vn";
        GovernmentReportHistoryDto result = reportService.submitReport(req, username);
        return ResponseEntity.ok(ApiResponse.ok("Gửi báo cáo lên UBND TP & Sở KH&CN thành công", result));
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

    /**
     * Xuất báo cáo định dạng PDF chính thức phục vụ báo cáo UBND TP & Sở TTTT.
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportGovernmentPdf(
            @RequestParam(defaultValue = "MONTH") String period
    ) {
        GovernmentReportDto report = reportService.generateReport(period);
        byte[] pdfBytes = pdfService.generatePdfReport(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Bao_cao_UBND_Hai_Phong_Chinh_Thuc.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
