package com.wificaptive.surveys.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.surveys.dto.SurveyDto;
import com.wificaptive.surveys.dto.SurveyResponseDto;
import com.wificaptive.surveys.dto.SurveyResponseSubmitRequest;
import com.wificaptive.surveys.service.SurveyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal/surveys")
public class SurveyPortalController {

    private final SurveyService surveyService;

    public SurveyPortalController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping("/{id}")
    public ApiResponse<SurveyDto> getSurveyForGuest(@PathVariable Long id) {
        return ApiResponse.ok(surveyService.getSurveyWithDetails(id));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<SurveyResponseDto> submitSurveyResponse(
            @PathVariable Long id,
            @Valid @RequestBody SurveyResponseSubmitRequest req) {
        return ApiResponse.ok("Gửi phản hồi khảo sát thành công. Cảm ơn quý khách!", surveyService.submitResponse(id, req));
    }
}

