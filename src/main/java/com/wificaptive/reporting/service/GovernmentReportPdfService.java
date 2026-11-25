package com.wificaptive.reporting.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.wificaptive.reporting.dto.DistrictStatDto;
import com.wificaptive.reporting.dto.GovernmentReportDto;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Xuất báo cáo định dạng PDF chuẩn văn thư hành chính cơ quan Nhà nước (Nghị định 30/2020/NĐ-CP).
 * Đồng bộ trực tiếp bố cục, tông màu sáng thanh lịch và toàn bộ số liệu thực với giao diện Web.
 */
@Service
public class GovernmentReportPdfService {

    private static final Color RED_800 = new Color(165, 45, 45);
    private static final Color RED_900 = new Color(148, 38, 38);
    private static final Color SLATE_900 = new Color(30, 41, 59);
    private static final Color SLATE_700 = new Color(71, 85, 105);
    private static final Color SLATE_600 = new Color(100, 116, 139);
    private static final Color SLATE_500 = new Color(120, 132, 148);
    private static final Color SLATE_400 = new Color(160, 172, 186);

    private static final Color BORDER_LIGHT = new Color(229, 234, 240);
    private static final Color BORDER_MEDIUM = new Color(214, 221, 230);
    private static final Color BG_LIGHT = new Color(250, 251, 253);
    private static final Color BG_TH = new Color(247, 248, 250);
    private static final Color BG_TOTAL = new Color(244, 246, 249);
    private static final Color BG_ZEBRA = Color.WHITE;

    private static final Color EMERALD_600 = new Color(22, 145, 102);
    private static final Color EMERALD_700 = new Color(21, 128, 88);
    private static final Color EMERALD_900 = new Color(45, 95, 76);
    private static final Color EMERALD_BG = new Color(246, 253, 249);
    private static final Color EMERALD_BORDER = new Color(212, 240, 225);

    private static final Color BLUE_600 = new Color(56, 112, 214);
    private static final Color BLUE_700 = new Color(45, 96, 196);
    private static final Color BLUE_900 = new Color(52, 82, 132);
    private static final Color BLUE_BG = new Color(246, 250, 255);
    private static final Color BLUE_BORDER = new Color(214, 228, 247);

    private static final Color INDIGO_600 = new Color(92, 88, 212);

    private static final Color AMBER_700 = new Color(185, 104, 34);
    private static final Color AMBER_900 = new Color(128, 88, 42);
    private static final Color AMBER_BG = new Color(255, 252, 245);
    private static final Color AMBER_BORDER = new Color(246, 231, 194);
    private static final Color ROSE_600 = new Color(225, 29, 72);

    public byte[] generatePdfReport(GovernmentReportDto report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 28, 28, 26, 24);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont bfRegular = getBaseFont("times.ttf");
            BaseFont bfBold = getBaseFont("timesbd.ttf");
            BaseFont bfItalic = getBaseFont("timesi.ttf");

            Font headerAgency = new Font(bfBold, 8.5f, Font.BOLD, SLATE_900);
            Font headerUnitRed = new Font(bfBold, 9f, Font.BOLD, RED_800);
            Font headerNational = new Font(bfBold, 8.5f, Font.BOLD, SLATE_900);
            Font headerMotto = new Font(bfBold, 8.5f, Font.BOLD, SLATE_900);
            Font headerMetaItalic = new Font(bfItalic, 7.8f, Font.ITALIC, SLATE_600);

            Font titleFont = new Font(bfBold, 13f, Font.BOLD, RED_900);
            Font subTitleBold = new Font(bfBold, 8.8f, Font.BOLD, SLATE_900);
            Font periodSubItalic = new Font(bfItalic, 7.8f, Font.ITALIC, SLATE_500);

            Font sectionTitleFont = new Font(bfBold, 8.8f, Font.BOLD, SLATE_900);
            Font kpiLabelFont = new Font(bfRegular, 7.5f, Font.NORMAL, SLATE_500);
            Font kpiSubFont = new Font(bfRegular, 6.8f, Font.NORMAL, SLATE_500);

            Font thFont = new Font(bfBold, 7.3f, Font.BOLD, SLATE_900);
            Font cellBold = new Font(bfBold, 7.3f, Font.BOLD, SLATE_900);
            Font cellRegular = new Font(bfRegular, 7.3f, Font.NORMAL, SLATE_700);
            Font cellMuted = new Font(bfRegular, 7.3f, Font.NORMAL, SLATE_400);
            Font cellEmerald = new Font(bfBold, 7.3f, Font.BOLD, EMERALD_700);
            Font cellBlue = new Font(bfBold, 7.3f, Font.BOLD, BLUE_700);

            Font noteBoldFont = new Font(bfBold, 7.5f, Font.BOLD, SLATE_900);
            Font noteTextFont = new Font(bfRegular, 7.3f, Font.NORMAL, SLATE_700);

            // 1. Quốc hiệu - Tiêu ngữ (Khớp 100% Web Header)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{48f, 52f});

            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setBorder(Rectangle.NO_BORDER);
            leftHeader.setPadding(0);
            leftHeader.addElement(new Paragraph("ỦY BAN NHÂN DÂN THÀNH PHỐ HẢI PHÒNG", headerAgency));
            Paragraph unitP = new Paragraph("SỞ KHOA HỌC VÀ CÔNG NGHỆ", headerUnitRed);
            unitP.setSpacingBefore(1);
            leftHeader.addElement(unitP);
            leftHeader.addElement(buildShortRule(95f, RED_800, Element.ALIGN_LEFT));
            Paragraph docNo = new Paragraph("Số: ....../BC-SKHCN", headerMetaItalic);
            docNo.setSpacingBefore(2);
            leftHeader.addElement(docNo);
            headerTable.addCell(leftHeader);

            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setBorder(Rectangle.NO_BORDER);
            rightHeader.setPadding(0);

            Paragraph nationalName = new Paragraph("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", headerNational);
            nationalName.setAlignment(Element.ALIGN_RIGHT);
            rightHeader.addElement(nationalName);

            Paragraph motto = new Paragraph("Độc lập - Tự do - Hạnh phúc", headerMotto);
            motto.setAlignment(Element.ALIGN_RIGHT);
            motto.setSpacingBefore(1);
            rightHeader.addElement(motto);
            rightHeader.addElement(buildShortRule(105f, SLATE_900, Element.ALIGN_RIGHT));

            ZonedDateTime genDate = (report.generatedAt() != null ? report.generatedAt() : Instant.now())
                    .atZone(ZoneId.of("Asia/Ho_Chi_Minh"));
            String dateFormatted = String.format("Hải Phòng, ngày %02d tháng %02d năm %d",
                    genDate.getDayOfMonth(), genDate.getMonthValue(), genDate.getYear());
            Paragraph dateP = new Paragraph(dateFormatted, headerMetaItalic);
            dateP.setAlignment(Element.ALIGN_RIGHT);
            dateP.setSpacingBefore(2);
            rightHeader.addElement(dateP);
            headerTable.addCell(rightHeader);

            document.add(headerTable);

            // Đường kẻ ngang phân cách đỏ tinh tế
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            divider.setSpacingBefore(6);
            PdfPCell divCell = new PdfPCell();
            divCell.setBorder(Rectangle.BOTTOM);
            divCell.setBorderColor(RED_800);
            divCell.setBorderWidth(0.8f);
            divCell.setFixedHeight(2);
            divider.addCell(divCell);
            document.add(divider);

            // 2. Tiêu đề báo cáo
            Paragraph title = new Paragraph("BÁO CÁO KỸ THUẬT & VẬN HÀNH HỆ THỐNG MẠNG WI-FI CÔNG CỘNG", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            document.add(title);

            Paragraph subTitle = new Paragraph(
                    "Đề án phủ sóng Wi-Fi miễn phí tại 1.584 Nhà văn hóa thôn, tổ dân phố trên địa bàn thành phố Hải Phòng",
                    subTitleBold
            );
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingBefore(3);
            document.add(subTitle);

            Paragraph periodP = new Paragraph(
                    "(Kỳ báo cáo: " + report.reportPeriod() + " — Dữ liệu vận hành thời gian thực từ hệ thống)",
                    periodSubItalic
            );
            periodP.setAlignment(Element.ALIGN_CENTER);
            periodP.setSpacingBefore(2);
            periodP.setSpacingAfter(9);
            document.add(periodP);

            // 3. Phần I: Chỉ số tổng thể toàn thành phố
            document.add(buildSectionHeading("I. ĐÁNH GIÁ CHỈ SỐ TỔNG THỂ TOÀN THÀNH PHỐ", sectionTitleFont));

            PdfPTable kpiRow1 = new PdfPTable(7);
            kpiRow1.setWidthPercentage(100);
            kpiRow1.setWidths(new float[]{23.5f, 1.5f, 23.5f, 1.5f, 23.5f, 1.5f, 25f});
            kpiRow1.setSpacingBefore(5);
            kpiRow1.setSpacingAfter(9);

            String dataFormatted = formatDataVolume(report.totalDataServedTb());
            String activeText = report.activeLocations() + " điểm Online";
            String subLoc = String.format("Quy hoạch: %,d điểm NVH", report.totalTargetLocations());

            Color availColor = report.networkAvailabilityRate() >= 99.0 ? EMERALD_600
                    : (report.networkAvailabilityRate() >= 80.0 ? AMBER_700 : ROSE_600);

            kpiRow1.addCell(buildKpiCard("Điểm phát trực tuyến", activeText, subLoc, BG_LIGHT, BORDER_LIGHT,
                    kpiLabelFont, new Font(bfBold, 11.5f, Font.BOLD, SLATE_900), new Font(bfBold, 6.8f, Font.NORMAL, SLATE_600)));
            kpiRow1.addCell(spacerCell());
            kpiRow1.addCell(buildKpiCard("Độ khả dụng mạng", formatPercent(report.networkAvailabilityRate()), "Chỉ tiêu đề án: > 99.0%", BG_LIGHT, BORDER_LIGHT,
                    kpiLabelFont, new Font(bfBold, 11.5f, Font.BOLD, availColor), kpiSubFont));
            kpiRow1.addCell(spacerCell());
            kpiRow1.addCell(buildKpiCard("Lượt người dân truy cập", String.format("%,d", report.totalCitizenSessions()), "Phiên đăng nhập thành công", BG_LIGHT, BORDER_LIGHT,
                    kpiLabelFont, new Font(bfBold, 11.5f, Font.BOLD, BLUE_600), kpiSubFont));
            kpiRow1.addCell(spacerCell());
            kpiRow1.addCell(buildKpiCard("Dung lượng dữ liệu", dataFormatted, "Lưu lượng qua Cổng chào", BG_LIGHT, BORDER_LIGHT,
                    kpiLabelFont, new Font(bfBold, 11.5f, Font.BOLD, INDIGO_600), kpiSubFont));

            document.add(kpiRow1);

            // 4. Phần II: Bảng chi tiết 15 quận / huyện (Đủ 9 cột, header xám nhạt thanh lịch giống web)
            document.add(buildSectionHeading("II. CHI TIẾT TỔNG HỢP THEO 15 QUẬN / HUYỆN THÀNH PHỐ HẢI PHÒNG", sectionTitleFont));

            PdfPTable dataTable = new PdfPTable(9);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[]{5.5f, 24.5f, 10.5f, 8f, 11f, 12.5f, 10.5f, 7.5f, 10f});
            dataTable.setSpacingBefore(5);
            dataTable.setHeaderRows(1);

            addTableHeader(dataTable, "STT", thFont, Element.ALIGN_CENTER);
            addTableHeader(dataTable, "Địa bàn Quận / Huyện", thFont, Element.ALIGN_LEFT);
            addTableHeader(dataTable, "Chỉ tiêu NVH", thFont, Element.ALIGN_CENTER);
            addTableHeader(dataTable, "Online", thFont, Element.ALIGN_CENTER);
            addTableHeader(dataTable, "Độ khả dụng", thFont, Element.ALIGN_CENTER);
            addTableHeader(dataTable, "Lượt truy cập", thFont, Element.ALIGN_RIGHT);
            addTableHeader(dataTable, "Dữ liệu (GB)", thFont, Element.ALIGN_RIGHT);
            addTableHeader(dataTable, "Sự cố", thFont, Element.ALIGN_CENTER);
            addTableHeader(dataTable, "Đạt SLA", thFont, Element.ALIGN_CENTER);

            int stt = 1;
            int totalPlanned = 0;
            int totalOnline = 0;
            long totalSessions = 0;
            double totalDataGb = 0.0;
            int totalIncidents = 0;

            if (report.districtBreakdown() != null) {
                for (DistrictStatDto d : report.districtBreakdown()) {
                    totalPlanned += d.totalLocations();
                    totalOnline += d.onlineLocations();
                    totalSessions += d.totalSessions();
                    totalDataGb += d.totalDataGb();
                    totalIncidents += d.totalIncidents();

                    boolean hasDeployedOrTraffic = d.onlineLocations() > 0 || d.totalSessions() > 0 || d.totalIncidents() > 0;
                    Color rowBg = (stt % 2 == 0) ? BG_ZEBRA : Color.WHITE;

                    dataTable.addCell(createDataCell(String.valueOf(stt++), cellMuted, Element.ALIGN_CENTER, rowBg));
                    dataTable.addCell(createDataCell(d.districtName(), cellBold, Element.ALIGN_LEFT, rowBg));
                    dataTable.addCell(createDataCell(String.valueOf(d.totalLocations()), cellRegular, Element.ALIGN_CENTER, rowBg));
                    dataTable.addCell(createDataCell(String.valueOf(d.onlineLocations()), d.onlineLocations() > 0 ? cellEmerald : cellMuted, Element.ALIGN_CENTER, rowBg));
                    dataTable.addCell(createDataCell(hasDeployedOrTraffic ? formatPercent(d.availabilityPercent()) : "—", hasDeployedOrTraffic ? cellRegular : cellMuted, Element.ALIGN_CENTER, rowBg));
                    dataTable.addCell(createDataCell(String.format("%,d", d.totalSessions()), d.totalSessions() > 0 ? cellBlue : cellMuted, Element.ALIGN_RIGHT, rowBg));
                    dataTable.addCell(createDataCell(String.format("%.2f", d.totalDataGb()), cellRegular, Element.ALIGN_RIGHT, rowBg));
                    dataTable.addCell(createDataCell(String.valueOf(d.totalIncidents()), d.totalIncidents() > 0 ? cellBold : cellMuted, Element.ALIGN_CENTER, rowBg));
                    dataTable.addCell(createDataCell(hasDeployedOrTraffic ? formatPercent(d.slaCompliancePercent()) : "—", hasDeployedOrTraffic ? cellEmerald : cellMuted, Element.ALIGN_CENTER, rowBg));
                }
            }

            // Dòng tổng cộng toàn thành phố
            PdfPCell totalLabel = createTotalCell("TỔNG CỘNG TOÀN THÀNH PHỐ", cellBold, Element.ALIGN_CENTER);
            totalLabel.setColspan(2);
            dataTable.addCell(totalLabel);
            dataTable.addCell(createTotalCell(String.format("%,d", totalPlanned), cellBold, Element.ALIGN_CENTER));
            dataTable.addCell(createTotalCell(String.valueOf(report.activeLocations() > 0 ? report.activeLocations() : totalOnline), cellEmerald, Element.ALIGN_CENTER));
            dataTable.addCell(createTotalCell(formatPercent(report.networkAvailabilityRate()), cellBold, Element.ALIGN_CENTER));
            dataTable.addCell(createTotalCell(String.format("%,d", report.totalCitizenSessions() > 0 ? report.totalCitizenSessions() : totalSessions), cellBlue, Element.ALIGN_RIGHT));
            dataTable.addCell(createTotalCell(String.format("%.2f", totalDataGb), cellBold, Element.ALIGN_RIGHT));
            dataTable.addCell(createTotalCell(String.valueOf(report.totalIncidentsReported() > 0 ? report.totalIncidentsReported() : totalIncidents), cellBold, Element.ALIGN_CENTER));
            Color slaColor = report.telecomSlaComplianceRate() >= 95.0 ? EMERALD_700
                    : (report.telecomSlaComplianceRate() >= 80.0 ? AMBER_700 : ROSE_600);
            Font totalSlaFont = new Font(bfBold, 7.3f, Font.BOLD, slaColor);
            dataTable.addCell(createTotalCell(formatPercent(report.telecomSlaComplianceRate()), totalSlaFont, Element.ALIGN_CENTER));

            document.add(dataTable);

            // 5. Phần III: Đánh giá ATTT & Truyền thông số (Khớp Web)
            PdfPTable part3Box = new PdfPTable(1);
            part3Box.setWidthPercentage(100);
            part3Box.setSpacingBefore(8);
            PdfPCell p3Cell = new PdfPCell();
            p3Cell.setBackgroundColor(BG_LIGHT);
            p3Cell.setBorderColor(BORDER_LIGHT);
            p3Cell.setBorderWidth(0.7f);
            p3Cell.setPadding(7);

            Paragraph p3Title = new Paragraph("III. ĐÁNH GIÁ HIỆN TRẠNG VẬN HÀNH & AN TOÀN THÔNG TIN",
                    new Font(bfBold, 8f, Font.BOLD, SLATE_900));
            p3Title.setSpacingAfter(3);
            p3Cell.addElement(p3Title);

            p3Cell.addElement(buildBullet("Cổng chào số & Dịch vụ công: ",
                    "Cổng chào điện tử phục vụ truyền thông chuyển đổi số và điều hướng trực tiếp tới Cổng Dịch vụ công thành phố Hải Phòng tại các điểm đã lắp đặt thiết bị.",
                    noteBoldFont, noteTextFont));
            p3Cell.addElement(buildBullet("An toàn thông tin & Lưu vết pháp lý: ",
                    "Hệ thống áp dụng phân quyền 3 cấp, kiểm soát tên miền DNS và lưu trữ nhật ký phiên truy cập 12 tháng phục vụ công tác kiểm tra, giám sát theo quy định.",
                    noteBoldFont, noteTextFont));
            String slaSummaryText = (report.networkAvailabilityRate() >= 99.0 && report.telecomSlaComplianceRate() >= 95.0)
                    ? String.format("Hệ thống vận hành ổn định, tỷ lệ sẵn sàng mạng đạt %s và tỷ lệ xử lý sự cố đúng hạn SLA đạt %s (MTTR: %.1f giờ).",
                            formatPercent(report.networkAvailabilityRate()), formatPercent(report.telecomSlaComplianceRate()), report.averageResolutionHours())
                    : String.format("Ghi nhận %d điểm đang phát sóng trực tuyến (độ khả dụng %s trên tổng số điểm đã khai báo); tiếp nhận %d sự cố, tỷ lệ xử lý đúng hạn SLA đạt %s (MTTR: %.1f giờ). Đề nghị các đơn vị đẩy nhanh tiến độ đấu nối thiết bị tại các điểm còn lại và xử lý dứt điểm các phiếu sự cố quá hạn.",
                            report.activeLocations(), formatPercent(report.networkAvailabilityRate()), report.totalIncidentsReported(), formatPercent(report.telecomSlaComplianceRate()), report.averageResolutionHours());
            p3Cell.addElement(buildBullet("Hiện trạng kết nối & Tuân thủ SLA trong kỳ: ",
                    slaSummaryText,
                    noteBoldFont, noteTextFont));

            part3Box.addCell(p3Cell);
            document.add(part3Box);

            // 6. Nơi nhận
            Paragraph recipientsTitle = new Paragraph("NƠI NHẬN:", new Font(bfBold, 7.8f, Font.BOLD, SLATE_900));
            recipientsTitle.setSpacingBefore(8);
            document.add(recipientsTitle);

            Font recFont = new Font(bfRegular, 7.3f, Font.NORMAL, SLATE_600);
            Paragraph recBody = new Paragraph(
                    "- UBND Thành phố Hải Phòng (để b/c);\n" +
                    "- Giám đốc Sở KH&CN (để chỉ đạo);\n" +
                    "- Viettel Hải Phòng (để phối hợp);\n" +
                    "- Lưu: VT, CNTT.",
                    recFont
            );
            recBody.setLeading(10.5f);
            document.add(recBody);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khởi tạo tài liệu PDF báo cáo cơ quan nhà nước: " + e.getMessage(), e);
        }
    }

    private static String formatDataVolume(double tb) {
        if (tb <= 0.0) return "0 MB";
        if (tb >= 1.0) return String.format("%.2f TB", tb);
        double gb = tb * 1024.0;
        if (gb >= 1.0) return String.format("%.2f GB", gb);
        double mb = gb * 1024.0;
        if (mb >= 0.1) return String.format("%.1f MB", mb);
        return String.format("%.0f KB", mb * 1024.0);
    }

    private static String formatPercent(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (rounded == Math.floor(rounded)) {
            return String.format("%.0f%%", rounded);
        }
        return String.format("%.1f%%", rounded);
    }

    private PdfPTable buildShortRule(float widthPoints, Color color, int align) {
        PdfPTable t = new PdfPTable(1);
        t.setTotalWidth(widthPoints);
        t.setLockedWidth(true);
        t.setHorizontalAlignment(align);
        t.setSpacingBefore(2);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(color);
        c.setBorderWidth(1f);
        c.setFixedHeight(2);
        t.addCell(c);
        return t;
    }

    private PdfPTable buildSectionHeading(String title, Font font) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(title, font));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDER_LIGHT);
        c.setBorderWidth(0.7f);
        c.setPaddingLeft(0);
        c.setPaddingRight(0);
        c.setPaddingTop(0);
        c.setPaddingBottom(3);
        t.addCell(c);
        return t;
    }

    private Paragraph buildBullet(String boldPrefix, String rest, Font boldFont, Font textFont) {
        Paragraph p = new Paragraph();
        p.setLeading(10f);
        p.setSpacingAfter(1.5f);
        p.add(new Chunk("•  ", boldFont));
        p.add(new Chunk(boldPrefix, boldFont));
        p.add(new Chunk(rest, textFont));
        return p;
    }

    private PdfPCell spacerCell() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private PdfPCell buildKpiCard(String label, String val, String sub, Color bg, Color borderColor,
                                  Font labelFont, Font valFont, Font subFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(borderColor);
        cell.setBorderWidth(0.45f);
        cell.setPaddingLeft(7);
        cell.setPaddingRight(7);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);

        Paragraph pLabel = new Paragraph(label, labelFont);
        pLabel.setLeading(9.5f);
        cell.addElement(pLabel);

        Paragraph pVal = new Paragraph(val, valFont);
        pVal.setSpacingBefore(3);
        pVal.setLeading(13.5f);
        cell.addElement(pVal);

        if (sub != null && !sub.isBlank()) {
            Paragraph pSub = new Paragraph(sub, subFont);
            pSub.setSpacingBefore(2);
            pSub.setLeading(8.5f);
            cell.addElement(pSub);
        }
        return cell;
    }

    private BaseFont getBaseFont(String fontName) {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/" + fontName);
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                return BaseFont.createFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
            }
            String winPath = "C:/Windows/Fonts/" + fontName;
            java.io.File f = new java.io.File(winPath);
            if (f.exists()) {
                return BaseFont.createFont(winPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        } catch (Exception e) {
            System.err.println("Không thể nạp font " + fontName + ": " + e.getMessage());
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void addTableHeader(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BG_TH);
        cell.setBorderColor(BORDER_MEDIUM);
        cell.setBorderWidth(0.4f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(4.5f);
        cell.setPaddingBottom(4.5f);
        cell.setPaddingLeft(4f);
        cell.setPaddingRight(4f);
        table.addCell(cell);
    }

    private PdfPCell createDataCell(String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER_LIGHT);
        cell.setBorderWidth(0.35f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(3.2f);
        cell.setPaddingBottom(3.2f);
        cell.setPaddingLeft(4f);
        cell.setPaddingRight(4f);
        return cell;
    }

    private PdfPCell createTotalCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BG_TOTAL);
        cell.setBorderColor(BORDER_LIGHT);
        cell.setBorderColorTop(BORDER_MEDIUM);
        cell.setBorderWidthTop(0.7f);
        cell.setBorderWidthBottom(0.4f);
        cell.setBorderWidthLeft(0.35f);
        cell.setBorderWidthRight(0.35f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(4f);
        cell.setPaddingRight(4f);
        return cell;
    }
}
