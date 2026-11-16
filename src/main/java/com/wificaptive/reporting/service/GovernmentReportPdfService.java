package com.wificaptive.reporting.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.wificaptive.reporting.dto.DistrictStatDto;
import com.wificaptive.reporting.dto.GovernmentReportDto;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Xuất báo cáo định dạng PDF chuẩn báo cáo cơ quan nhà nước
 * (Quốc hiệu, Tiêu ngữ, Bảng tổng hợp số liệu 15 quận huyện TP. Hải Phòng).
 */
@Service
public class GovernmentReportPdfService {

    public byte[] generatePdfReport(GovernmentReportDto report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Font styles
            Font headerBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font headerRegular = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(0, 75, 135));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.DARK_GRAY);
            Font cellBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
            Font cellRegular = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

            // Quốc hiệu - Tiêu ngữ Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{50, 50});

            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setBorder(Rectangle.NO_BORDER);
            leftHeader.addElement(new Paragraph("UBND THANH PHO HAI PHONG", headerRegular));
            leftHeader.addElement(new Paragraph("SO THONG TIN VA TRUYEN THONG", headerBold));
            leftHeader.addElement(new Paragraph("So: ...../BC-STTTT-CNTT", subTitleFont));
            headerTable.addCell(leftHeader);

            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setBorder(Rectangle.NO_BORDER);
            rightHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph nationalName = new Paragraph("CONG HOA XA HOI CHU NGHIA VIET NAM", headerBold);
            nationalName.setAlignment(Element.ALIGN_CENTER);
            Paragraph motto = new Paragraph("Doc lap - Tu do - Hanh phuc", headerBold);
            motto.setAlignment(Element.ALIGN_CENTER);
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            Paragraph dateP = new Paragraph("Hai Phong, ngay " + todayStr, subTitleFont);
            dateP.setAlignment(Element.ALIGN_CENTER);

            rightHeader.addElement(nationalName);
            rightHeader.addElement(motto);
            rightHeader.addElement(dateP);
            headerTable.addCell(rightHeader);

            document.add(headerTable);
            document.add(new Paragraph(" ", subTitleFont));

            // Tiêu đề báo cáo
            Paragraph title = new Paragraph("BAO CAO TONG HOP TINH HINH VAN HANH HE THONG WI-FI CONG CONG", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subTitle = new Paragraph("De an phu song Wi-Fi mien phi tai 1.584 Nha van hoa thon/to dan pho tren dia ban TP. Hai Phong\n(Ky bao cao: " + report.reportPeriod() + ")", subTitleFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subTitle);
            document.add(new Paragraph(" ", subTitleFont));

            // Chỉ số KPI cốt lõi
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setWidths(new float[]{25, 25, 25, 25});

            addKpiCell(kpiTable, "Tong so diem NVH", report.totalTargetLocations() + " diem", cellRegular, cellBold);
            addKpiCell(kpiTable, "Ty le san sang (SLA)", String.format("%.2f%%", report.networkAvailabilityRate()), cellRegular, cellBold);
            addKpiCell(kpiTable, "Tong luot truy cap", String.format("%,d luot", report.totalCitizenSessions()), cellRegular, cellBold);
            addKpiCell(kpiTable, "Dung luong phuc vu", String.format("%.2f TB", report.totalDataServedTb()), cellRegular, cellBold);
            document.add(kpiTable);
            document.add(new Paragraph(" ", subTitleFont));

            // Bảng chi tiết 15 quận/huyện
            Paragraph tableTitle = new Paragraph("BANG TONG HOP SO LIEU THEO 15 QUAN / HUYEN:", headerBold);
            document.add(tableTitle);
            document.add(new Paragraph(" ", subTitleFont));

            PdfPTable dataTable = new PdfPTable(7);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[]{8, 26, 13, 13, 14, 13, 13});

            // Headers
            Color headerBg = new Color(0, 75, 135);
            addTableHeader(dataTable, "STT", tableHeaderFont, headerBg);
            addTableHeader(dataTable, "Quan / Huyen", tableHeaderFont, headerBg);
            addTableHeader(dataTable, "Tong diem", tableHeaderFont, headerBg);
            addTableHeader(dataTable, "Hoat dong", tableHeaderFont, headerBg);
            addTableHeader(dataTable, "Ty le %", tableHeaderFont, headerBg);
            addTableHeader(dataTable, "Luot ket noi", tableHeaderFont, headerBg);
            addTableHeader(dataTable, "Su co", tableHeaderFont, headerBg);

            int stt = 1;
            if (report.districtBreakdown() != null) {
                for (DistrictStatDto d : report.districtBreakdown()) {
                    dataTable.addCell(createCell(String.valueOf(stt++), cellRegular, Element.ALIGN_CENTER));
                    dataTable.addCell(createCell(d.districtName(), cellRegular, Element.ALIGN_LEFT));
                    dataTable.addCell(createCell(String.valueOf(d.totalLocations()), cellRegular, Element.ALIGN_RIGHT));
                    dataTable.addCell(createCell(String.valueOf(d.onlineLocations()), cellRegular, Element.ALIGN_RIGHT));
                    dataTable.addCell(createCell(String.format("%.1f%%", d.availabilityPercent()), cellRegular, Element.ALIGN_RIGHT));
                    dataTable.addCell(createCell(String.format("%,d", d.totalSessions()), cellRegular, Element.ALIGN_RIGHT));
                    dataTable.addCell(createCell(String.valueOf(d.totalIncidents()), cellRegular, Element.ALIGN_CENTER));
                }
            }
            document.add(dataTable);
            document.add(new Paragraph(" ", subTitleFont));

            // Chữ ký và nơi nhận
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{50, 50});

            PdfPCell leftFooter = new PdfPCell();
            leftFooter.setBorder(Rectangle.NO_BORDER);
            leftFooter.addElement(new Paragraph("Noi nhan:", cellBold));
            leftFooter.addElement(new Paragraph("- UBND Thanh pho (de b/c);", cellRegular));
            leftFooter.addElement(new Paragraph("- Giam doc So TTTT (de chi dao);", cellRegular));
            leftFooter.addElement(new Paragraph("- Luu: VT, CNTT.", cellRegular));
            footerTable.addCell(leftFooter);

            PdfPCell rightFooter = new PdfPCell();
            rightFooter.setBorder(Rectangle.NO_BORDER);
            rightFooter.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph signTitle = new Paragraph("KT. GIAM DOC\nPHO GIAM DOC", headerBold);
            signTitle.setAlignment(Element.ALIGN_CENTER);
            Paragraph signSpace = new Paragraph("\n\n\n(Ky, ghi ro ho ten va dong dau)", subTitleFont);
            signSpace.setAlignment(Element.ALIGN_CENTER);
            rightFooter.addElement(signTitle);
            rightFooter.addElement(signSpace);
            footerTable.addCell(rightFooter);

            document.add(footerTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khởi tạo tài liệu PDF báo cáo cơ quan nhà nước: " + e.getMessage(), e);
        }
    }

    private void addTableHeader(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    private void addKpiCell(PdfPTable table, String label, String val, Font labelFont, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(245, 247, 250));
        cell.setPadding(6);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(val, valFont));
        table.addCell(cell);
    }
}
