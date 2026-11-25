package com.wificaptive.gateway.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.security.UserPrincipal;
import com.wificaptive.gateway.adapter.GatewayAdapter;
import com.wificaptive.gateway.dto.GatewayStatusDto;
import com.wificaptive.gateway.dto.MockGatewayConfigDto;
import com.wificaptive.gateway.entity.GatewayTransaction;
import com.wificaptive.gateway.repository.GatewayTransactionRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/gateway")
@PreAuthorize("@rbac.hasPermission('SETTINGS', 'VIEW')")
public class GatewayAdminController {

    private final GatewayAdapter gatewayAdapter;
    private final com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter gwnCloudGatewayAdapter;
    private final GatewayTransactionRepository transactionRepository;
    private final AuditService auditService;

    public GatewayAdminController(
            GatewayAdapter gatewayAdapter,
            com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter gwnCloudGatewayAdapter,
            GatewayTransactionRepository transactionRepository,
            AuditService auditService) {
        this.gatewayAdapter = gatewayAdapter;
        this.gwnCloudGatewayAdapter = gwnCloudGatewayAdapter;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    @GetMapping("/status")
    public ApiResponse<GatewayStatusDto> getGatewayStatus() {
        return ApiResponse.ok(gatewayAdapter.getStatus());
    }

    @PreAuthorize("@rbac.hasPermission('SETTINGS', 'EDIT')")
    @PostMapping("/config")
    public ApiResponse<GatewayStatusDto> updateConfig(
            @Valid @RequestBody MockGatewayConfigDto config,
            @AuthenticationPrincipal UserPrincipal principal) {
        GatewayStatusDto updated = gatewayAdapter.updateConfig(config);

        auditService.recordEvent(
                AuditAction.GATEWAY_TOGGLE,
                principal != null ? principal.getUsername() : "admin",
                "internal",
                "GATEWAY",
                "MOCK",
                String.format("Updated Mock Gateway config: simulateFailures=%s, latencyMs=%s",
                        config.getSimulateFailures(), config.getSimulatedLatencyMs())
        );

        return ApiResponse.ok("Gateway simulation settings updated", updated);
    }

    @PreAuthorize("@rbac.hasPermission('SETTINGS', 'EDIT')")
    @PostMapping("/portal-bypass")
    public ApiResponse<GatewayStatusDto> togglePortalBypass(
            @RequestParam boolean active,
            @RequestParam(required = false, defaultValue = "Kích hoạt thủ công bởi Quản trị viên") String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        gwnCloudGatewayAdapter.setEmergencyPortalBypass(active, reason);

        auditService.recordEvent(
                AuditAction.GATEWAY_TOGGLE,
                principal != null ? principal.getUsername() : "admin",
                "internal",
                "GATEWAY",
                "GWN_CLOUD",
                String.format("Thay đổi chế độ Bỏ qua Cổng chào (Portal Bypass Fail-Open): active=%s, lý do=%s", active, reason)
        );

        String msg = active
                ? "Đã BẬT chế độ Bỏ qua Cổng chào (Fail-Open): Người dân vào thẳng mạng không cần đăng nhập"
                : "Đã TẮT chế độ Bỏ qua Cổng chào: Khôi phục Cổng chào bình thường";
        return ApiResponse.ok(msg, gatewayAdapter.getStatus());
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<GatewayTransaction>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<GatewayTransaction> txPage = transactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ApiResponse.ok(PageResponse.from(txPage));
    }
}
