package com.wificaptive.analytics.controller;

import com.wificaptive.analytics.dto.DashboardOverviewDto;
import com.wificaptive.analytics.dto.HourlyTrendDto;
import com.wificaptive.analytics.service.AnalyticsService;
import com.wificaptive.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/analytics", "/api/admin/dashboard"})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewDto> getOverview() {
        return ApiResponse.ok(analyticsService.getOverview());
    }

    @GetMapping("/trends")
    public ApiResponse<List<HourlyTrendDto>> getHourlyTrends() {
        return ApiResponse.ok(analyticsService.getHourlyTrends());
    }
}
