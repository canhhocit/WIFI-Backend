package com.wificaptive.surveys.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.surveys.dto.SurveyAnalyticsDto;
import com.wificaptive.surveys.entity.Survey;
import com.wificaptive.surveys.entity.SurveyQuestion;
import com.wificaptive.surveys.entity.SurveyResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SurveyExcelService {

    private static final Logger log = LoggerFactory.getLogger(SurveyExcelService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final ObjectMapper objectMapper;
    private final WifiLocationRepository locationRepository;

    public SurveyExcelService(ObjectMapper objectMapper, WifiLocationRepository locationRepository) {
        this.objectMapper = objectMapper;
        this.locationRepository = locationRepository;
    }

    public byte[] generateSurveyExcel(
            Survey survey,
            List<SurveyQuestion> questions,
            List<SurveyResponse> responses,
            SurveyAnalyticsDto analytics
    ) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Build location cache
            Map<Long, String> locationNameMap = new HashMap<>();
            for (WifiLocation loc : locationRepository.findAll()) {
                locationNameMap.put(loc.getId(), loc.getName());
            }

            // Cell Styles
            XSSFCellStyle titleStyle = createTitleStyle(workbook);
            XSSFCellStyle metaStyle = createMetaStyle(workbook);
            XSSFCellStyle headerStyle = createHeaderStyle(workbook);
            XSSFCellStyle dataStyle = createDataStyle(workbook);
            XSSFCellStyle centerDataStyle = createCenterDataStyle(workbook);
            XSSFCellStyle sectionHeaderStyle = createSectionHeaderStyle(workbook);
            XSSFCellStyle percentDataStyle = createPercentDataStyle(workbook);

            // ================= SHEET 1: DANH SÁCH CHI TIẾT =================
            Sheet sheet1 = workbook.createSheet("Danh_Sach_Phan_Hoi");
            sheet1.setDisplayGridlines(true);

            int rowIdx = 0;

            // Title block
            Row titleRow = sheet1.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO CHI TIẾT PHẢN HỒI Ý KIẾN NHÂN DÂN");
            titleCell.setCellStyle(titleStyle);
            sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(5, questions.size() + 4)));

            Row subRow = sheet1.createRow(rowIdx++);
            Cell subCell = subRow.createCell(0);
            subCell.setCellValue("Bài khảo sát: " + survey.getTitle() + " | Tổng số phản hồi: " + responses.size() + " phiếu");
            subCell.setCellStyle(metaStyle);
            sheet1.addMergedRegion(new CellRangeAddress(1, 1, 0, Math.max(5, questions.size() + 4)));

            rowIdx++; // Blank line

            // Header row
            Row headerRow = sheet1.createRow(rowIdx++);
            headerRow.setHeightInPoints(26);

            int col = 0;
            createHeaderCell(headerRow, col++, "STT", headerStyle);
            createHeaderCell(headerRow, col++, "Mã Phiếu", headerStyle);
            createHeaderCell(headerRow, col++, "Thời Gian Gửi", headerStyle);
            createHeaderCell(headerRow, col++, "Điểm Nhà Văn Hóa", headerStyle);
            createHeaderCell(headerRow, col++, "Địa Chỉ MAC", headerStyle);

            Map<Long, Integer> questionColMap = new HashMap<>();
            for (SurveyQuestion q : questions) {
                questionColMap.put(q.getId(), col);
                createHeaderCell(headerRow, col++, q.getQuestionText(), headerStyle);
            }

            int stt = 1;
            for (SurveyResponse r : responses) {
                Row dataRow = sheet1.createRow(rowIdx++);
                int c = 0;
                createCell(dataRow, c++, String.valueOf(stt++), centerDataStyle);
                createCell(dataRow, c++, "#" + r.getId(), centerDataStyle);
                createCell(dataRow, c++, r.getSubmittedAt() != null ? DATE_TIME_FORMATTER.format(r.getSubmittedAt()) : "—", centerDataStyle);

                String locName = r.getLocationId() != null ? locationNameMap.getOrDefault(r.getLocationId(), "Chung hệ thống") : "Chung hệ thống";
                createCell(dataRow, c++, locName, dataStyle);
                createCell(dataRow, c++, r.getClientMac() != null ? r.getClientMac().toUpperCase() : "—", centerDataStyle);

                Map<String, Object> answers = parseAnswers(r.getAnswersJson());
                for (SurveyQuestion q : questions) {
                    Object ansVal = answers.get(q.getId().toString());
                    String formattedAns = formatAnswerValue(ansVal);
                    createCell(dataRow, c++, formattedAns, dataStyle);
                }
            }

            // Auto-size columns for sheet 1
            for (int i = 0; i < col; i++) {
                sheet1.autoSizeColumn(i);
                int currentWidth = sheet1.getColumnWidth(i);
                if (currentWidth < 3000) sheet1.setColumnWidth(i, 3000);
                if (currentWidth > 15000) sheet1.setColumnWidth(i, 15000);
            }

            // ================= SHEET 2: THỐNG KÊ TỔNG HỢP =================
            Sheet sheet2 = workbook.createSheet("Thong_Ke_Tong_Hop");
            sheet2.setDisplayGridlines(true);

            int s2Row = 0;

            Row s2TitleRow = sheet2.createRow(s2Row++);
            Cell s2TitleCell = s2TitleRow.createCell(0);
            s2TitleCell.setCellValue("TỔNG HỢP KẾT QUẢ KHẢO SÁT & TỶ LỆ PHẦN TRĂM");
            s2TitleCell.setCellStyle(titleStyle);
            sheet2.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row s2SubRow = sheet2.createRow(s2Row++);
            Cell s2SubCell = s2SubRow.createCell(0);
            s2SubCell.setCellValue("Đơn vị quản lý: UBND Thành phố Hải Phòng • Cổng Thông Tin Wi-Fi Nhà văn hóa");
            s2SubCell.setCellStyle(metaStyle);
            sheet2.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            s2Row++; // Blank

            // KPI Summary table
            Row kpiHeader = sheet2.createRow(s2Row++);
            createHeaderCell(kpiHeader, 0, "Chỉ Số Đo Lường", headerStyle);
            createHeaderCell(kpiHeader, 1, "Giá Trị", headerStyle);
            createHeaderCell(kpiHeader, 2, "Ghi Chú", headerStyle);

            addKpiRow(sheet2, s2Row++, "Tên bài khảo sát", survey.getTitle(), "Tiêu đề thăm dò ý kiến", dataStyle, centerDataStyle);
            addKpiRow(sheet2, s2Row++, "Tổng số phiếu thu thập", String.valueOf(analytics.getTotalResponses()), "Phiếu thực tế từ người dân", dataStyle, centerDataStyle);
            addKpiRow(sheet2, s2Row++, "Hạn ngạch định mức (Quota)", String.valueOf(analytics.getMaxResponsesQuota()), "Chỉ tiêu tối đa", dataStyle, centerDataStyle);
            addKpiRow(sheet2, s2Row++, "Tỷ lệ hoàn thành hạn ngạch", String.format("%.1f%%", analytics.getCompletionRatePercent()), "Tiến độ khảo sát", dataStyle, centerDataStyle);
            if (analytics.getAverageRating() != null) {
                addKpiRow(sheet2, s2Row++, "Điểm đánh giá trung bình (CSAT)", String.format("%.2f / 5.0 ★", analytics.getAverageRating()), "Mức độ hài lòng của công dân", dataStyle, centerDataStyle);
            }

            s2Row += 2; // Blank

            // Detailed question breakdown
            Row qBreakHeader = sheet2.createRow(s2Row++);
            createHeaderCell(qBreakHeader, 0, "STT Câu Hỏi", sectionHeaderStyle);
            createHeaderCell(qBreakHeader, 1, "Nội Dung Câu Hỏi", sectionHeaderStyle);
            createHeaderCell(qBreakHeader, 2, "Loại Câu Hỏi", sectionHeaderStyle);
            createHeaderCell(qBreakHeader, 3, "Lựa Chọn / Thang Đo", sectionHeaderStyle);
            createHeaderCell(qBreakHeader, 4, "Số Lượt Chọn", sectionHeaderStyle);
            createHeaderCell(qBreakHeader, 5, "Tỷ Lệ (%)", sectionHeaderStyle);

            int qIdx = 1;
            for (SurveyAnalyticsDto.QuestionAnalyticsDto qa : analytics.getQuestionAnalytics()) {
                if ("TEXT".equalsIgnoreCase(qa.getQuestionType())) {
                    Row textRow = sheet2.createRow(s2Row++);
                    createCell(textRow, 0, "Câu " + qIdx, centerDataStyle);
                    createCell(textRow, 1, qa.getQuestionText(), dataStyle);
                    createCell(textRow, 2, "Ý kiến tự luận", centerDataStyle);
                    createCell(textRow, 3, "Ý kiến đóng góp bằng văn bản", dataStyle);
                    createCell(textRow, 4, String.valueOf(qa.getTextResponses().size()), centerDataStyle);
                    createCell(textRow, 5, "—", centerDataStyle);
                } else if ("RATING".equalsIgnoreCase(qa.getQuestionType())) {
                    for (int star = 5; star >= 1; star--) {
                        Row ratingRow = sheet2.createRow(s2Row++);
                        createCell(ratingRow, 0, star == 5 ? "Câu " + qIdx : "", centerDataStyle);
                        createCell(ratingRow, 1, star == 5 ? qa.getQuestionText() : "", dataStyle);
                        createCell(ratingRow, 2, star == 5 ? "Đánh giá 1-5 Sao" : "", centerDataStyle);
                        createCell(ratingRow, 3, star + " Sao (" + getStarLabel(star) + ")", dataStyle);
                        long count = qa.getRatingCounts().getOrDefault(star, 0L);
                        createCell(ratingRow, 4, String.valueOf(count), centerDataStyle);
                        double pct = qa.getTotalAnswersCount() > 0 ? (count * 100.0 / qa.getTotalAnswersCount()) : 0.0;
                        createCell(ratingRow, 5, String.format("%.1f%%", pct), percentDataStyle);
                    }
                } else {
                    boolean isFirst = true;
                    for (Map.Entry<String, Long> entry : qa.getOptionCounts().entrySet()) {
                        Row optRow = sheet2.createRow(s2Row++);
                        createCell(optRow, 0, isFirst ? "Câu " + qIdx : "", centerDataStyle);
                        createCell(optRow, 1, isFirst ? qa.getQuestionText() : "", dataStyle);
                        createCell(optRow, 2, isFirst ? ("MULTI_CHOICE".equalsIgnoreCase(qa.getQuestionType()) ? "Chọn nhiều" : "Trắc nghiệm chọn 1") : "", centerDataStyle);
                        createCell(optRow, 3, entry.getKey(), dataStyle);
                        createCell(optRow, 4, String.valueOf(entry.getValue()), centerDataStyle);
                        double pct = qa.getOptionPercentages().getOrDefault(entry.getKey(), 0.0);
                        createCell(optRow, 5, String.format("%.1f%%", pct), percentDataStyle);
                        isFirst = false;
                    }
                }
                qIdx++;
            }

            s2Row += 2; // Blank

            // Location distribution table
            Row locHeader = sheet2.createRow(s2Row++);
            createHeaderCell(locHeader, 0, "STT", sectionHeaderStyle);
            createHeaderCell(locHeader, 1, "Điểm Nhà Văn Hóa Thôn / Tổ Dân Phố", sectionHeaderStyle);
            createHeaderCell(locHeader, 2, "Số Lượng Phản Hồi", sectionHeaderStyle);
            createHeaderCell(locHeader, 3, "Tỷ Lệ Đóng Góp (%)", sectionHeaderStyle);

            int locStt = 1;
            for (Map.Entry<String, Long> entry : analytics.getLocationDistribution().entrySet()) {
                Row locRow = sheet2.createRow(s2Row++);
                createCell(locRow, 0, String.valueOf(locStt++), centerDataStyle);
                createCell(locRow, 1, entry.getKey(), dataStyle);
                createCell(locRow, 2, String.valueOf(entry.getValue()), centerDataStyle);
                double pct = analytics.getTotalResponses() > 0 ? (entry.getValue() * 100.0 / analytics.getTotalResponses()) : 0.0;
                createCell(locRow, 3, String.format("%.1f%%", pct), percentDataStyle);
            }

            for (int i = 0; i <= 5; i++) {
                sheet2.autoSizeColumn(i);
                int currentWidth = sheet2.getColumnWidth(i);
                if (currentWidth < 3500) sheet2.setColumnWidth(i, 3500);
                if (currentWidth > 16000) sheet2.setColumnWidth(i, 16000);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate Excel report for survey {}: {}", survey.getId(), e.getMessage(), e);
            throw new RuntimeException("Lỗi sinh file Excel báo cáo khảo sát", e);
        }
    }

    private String getStarLabel(int star) {
        return switch (star) {
            case 5 -> "Rất hài lòng";
            case 4 -> "Hài lòng";
            case 3 -> "Bình thường";
            case 2 -> "Chưa hài lòng";
            default -> "Rất kém";
        };
    }

    private void addKpiRow(Sheet sheet, int rowIdx, String label, String value, String note, CellStyle labelStyle, CellStyle valStyle) {
        Row row = sheet.createRow(rowIdx);
        createCell(row, 0, label, labelStyle);
        createCell(row, 1, value, valStyle);
        createCell(row, 2, note, labelStyle);
    }

    private Map<String, Object> parseAnswers(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String formatAnswerValue(Object val) {
        if (val == null) return "—";
        if (val instanceof List<?> list) {
            return String.join(", ", list.stream().map(Object::toString).toList());
        }
        return val.toString();
    }

    private void createHeaderCell(Row row, int col, String text, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(text);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int col, String text, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(text != null ? text : "—");
        cell.setCellStyle(style);
    }

    private XSSFCellStyle createTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 14);
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 12, (byte) 35, (byte) 64}, null)); // #0c2340 Navy
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private XSSFCellStyle createMetaStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 71, (byte) 85, (byte) 105}, null)); // Slate 600
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 12, (byte) 35, (byte) 64}, null)); // Navy #0C2340
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createSectionHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 2, (byte) 84, (byte) 146}, null)); // Blue #025492
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createCenterDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createPercentDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 9);
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 5, (byte) 150, (byte) 105}, null)); // Emerald 600
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}

