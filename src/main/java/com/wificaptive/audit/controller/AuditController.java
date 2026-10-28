package com.wificaptive.audit.controller;

import com.wificaptive.audit.dto.AuditEventDto;
import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditEventDto>> getAuditEvents(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        PageResponse<AuditEventDto> response = auditService.getAuditEvents(action, pageable);
        return ApiResponse.ok(response);
    }
}
