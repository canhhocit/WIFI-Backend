package com.wificaptive.gateway.controller;

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
public class GatewayAdminController {

    private final GatewayAdapter gatewayAdapter;
    private final GatewayTransactionRepository transactionRepository;
    private final AuditService auditService;

    public GatewayAdminController(GatewayAdapter gatewayAdapter, GatewayTransactionRepository transactionRepository, AuditService auditService) {
        this.gatewayAdapter = gatewayAdapter;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    @GetMapping("/status")
    public ApiResponse<GatewayStatusDto> getGatewayStatus() {
        return ApiResponse.ok(gatewayAdapter.getStatus());
    }

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

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<GatewayTransaction>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<GatewayTransaction> txPage = transactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ApiResponse.ok(PageResponse.from(txPage));
    }
}
