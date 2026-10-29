package com.wificaptive.monitoring.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.monitoring.dto.TelemetryIngestRequest;
import com.wificaptive.monitoring.dto.TelemetryMetricDto;
import com.wificaptive.monitoring.service.MonitoringService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/monitoring")
public class TelemetryController {

    private final MonitoringService monitoringService;

    public TelemetryController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @PostMapping("/telemetry")
    public ApiResponse<TelemetryMetricDto> ingestTelemetry(@Valid @RequestBody TelemetryIngestRequest req) {
        return ApiResponse.ok("Ghi nhận thông số telemetry thành công", monitoringService.recordTelemetry(req));
    }

    @GetMapping("/locations/{locationId}/latest")
    public ApiResponse<List<TelemetryMetricDto>> getLatestMetrics(
            @PathVariable Long locationId,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(monitoringService.getLatestMetricsForLocation(locationId, limit));
    }

    @GetMapping("/locations/{locationId}/root-cause")
    public ApiResponse<String> getRootCauseAnalysis(@PathVariable Long locationId) {
        return ApiResponse.ok("Phân tích nguyên nhân gốc rễ", monitoringService.analyzeRootCause(locationId));
    }
}

