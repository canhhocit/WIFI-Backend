package com.wificaptive.network.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.network.dto.AccessLogRetentionDto;
import com.wificaptive.network.service.AccessLogRetentionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping({"/api/v1/admin/access-logs", "/v1/admin/access-logs"})
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN')")
public class AccessLogRetentionController {

    private final AccessLogRetentionService retentionService;

    public AccessLogRetentionController(AccessLogRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AccessLogRetentionDto>>> searchLogs(
            @RequestParam(required = false) String clientMac,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AccessLogRetentionDto> results = retentionService.searchLogs(
                clientMac,
                locationId,
                startDate,
                endDate,
                PageRequest.of(page, size, Sort.by("authenticatedAt").descending())
        );
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(results)));
    }
}

