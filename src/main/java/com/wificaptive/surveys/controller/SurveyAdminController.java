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
}

