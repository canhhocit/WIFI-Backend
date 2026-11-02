package com.wificaptive.surveys.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.surveys.dto.SurveyDto;
import com.wificaptive.surveys.dto.SurveyResponseDto;
import com.wificaptive.surveys.service.SurveyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/surveys")
public class SurveyAdminController {

    private final SurveyService surveyService;

    public SurveyAdminController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SurveyDto>> getSurveys(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.ok(surveyService.getSurveys(active, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SurveyDto> getSurveyById(@PathVariable Long id) {
        return ApiResponse.ok(surveyService.getSurveyWithDetails(id));
    }

    @PostMapping
    public ApiResponse<SurveyDto> createSurvey(@Valid @RequestBody SurveyDto dto) {
        return ApiResponse.ok("Tạo bài khảo sát thành công", surveyService.createSurvey(dto));
    }

    @GetMapping("/{id}/responses")
    public ApiResponse<PageResponse<SurveyResponseDto>> getSurveyResponses(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(surveyService.getResponses(id, page, size));
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<SurveyDto> toggleSurvey(@PathVariable Long id) {
        return ApiResponse.ok("Chuyển trạng thái khảo sát thành công", surveyService.toggleSurvey(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSurvey(@PathVariable Long id) {
        surveyService.deleteSurvey(id);
        return ApiResponse.ok("Xóa bài khảo sát thành công", null);
    }

    @GetMapping("/{id}/export/csv")
    public org.springframework.http.ResponseEntity<byte[]> exportResponsesCsv(@PathVariable Long id) {
        String csv = surveyService.exportResponsesCsv(id);
        byte[] csvBytes = ("\uFEFF" + csv).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Ket_qua_khao_sat_" + id + ".csv\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }
}

