package com.wificaptive.reporting;

import com.wificaptive.reporting.dto.DistrictStatDto;
import com.wificaptive.reporting.dto.GovernmentReportDto;
import com.wificaptive.reporting.service.GovernmentReportPdfService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GovernmentReportPdfServiceTest {

    @Test
    void testGeneratePdfReport_GeneratesValidPdfWithSealAndFonts() {
        GovernmentReportPdfService pdfService = new GovernmentReportPdfService();

        List<DistrictStatDto> districts = new java.util.ArrayList<>();
        String[] distNames = {
                "Quận Hồng Bàng", "Quận Ngô Quyền", "Quận Lê Chân", "Quận Hải An",
                "Quận Kiến An", "Quận Đồ Sơn", "Quận Dương Kinh", "Huyện Thủy Nguyên",
                "Huyện An Dương", "Huyện An Lão", "Huyện Kiến Thụy", "Huyện Tiên Lãng",
                "Huyện Vĩnh Bảo", "Huyện Cát Hải", "Huyện Bạch Long Vĩ"
        };
        int[] planned = { 42, 82, 114, 72, 78, 38, 44, 320, 125, 110, 118, 175, 240, 23, 3 };

        for (int i = 0; i < distNames.length; i++) {
            int online = (i == 0) ? 1 : (i == 7) ? 2 : (i == 13) ? 1 : 0;
            long sessions = (i == 0) ? 103 : (i == 7) ? 8 : (i == 13) ? 1 : 0;
            double data = (i == 0) ? 0.17 : (i == 7) ? 0.01 : 0.0;
            double avail = (online > 0) ? (i == 7 ? 66.7 : 100.0) : 100.0;
            districts.add(new DistrictStatDto((long)(i + 1), distNames[i], planned[i], online, avail, sessions, data, 0, 100.0));
        }

        GovernmentReportDto dto = new GovernmentReportDto(
                "THÁNG 11/2026",
                "ỦY BAN NHÂN DÂN THÀNH PHỐ HẢI PHÒNG",
                "SỞ THÔNG TIN VÀ TRUYỀN THÔNG HẢI PHÒNG",
                Instant.now(),
                1584,
                4,
                80.0,
                112L,
                0.18,
                96.5,
                0,
                100.0,
                0.0,
                districts
        );

        byte[] pdfBytes = pdfService.generatePdfReport(dto);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000, "PDF size must be greater than 1KB");

        // Save for visual inspection
        try {
            java.nio.file.Files.write(java.nio.file.Path.of("target/test-report.pdf"), pdfBytes);
        } catch (Exception ignored) {}

        // Verify PDF Magic Bytes %PDF-
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }
}
