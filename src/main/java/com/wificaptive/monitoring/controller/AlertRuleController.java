package com.wificaptive.monitoring.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.monitoring.dto.AlertRuleDto;
import com.wificaptive.monitoring.service.MonitoringService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/alerts")
public class AlertRuleController {

    private final MonitoringService monitoringService;

    public AlertRuleController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/rules")
    public ApiResponse<List<AlertRuleDto>> getAlertRules() {
        return ApiResponse.ok(monitoringService.getAllAlertRules());
    }

    @PatchMapping("/rules/{id}/toggle")
    public ApiResponse<AlertRuleDto> toggleRule(@PathVariable Long id, @RequestParam boolean enabled) {
        return ApiResponse.ok("Cập nhật trạng thái quy tắc cảnh báo thành công", monitoringService.updateAlertRuleStatus(id, enabled));
    }
}

