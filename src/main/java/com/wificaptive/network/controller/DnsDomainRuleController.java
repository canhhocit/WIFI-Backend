package com.wificaptive.network.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.network.dto.CreateDnsDomainRuleRequest;
import com.wificaptive.network.dto.DnsDomainRuleDto;
import com.wificaptive.network.service.DnsDomainRuleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dns-domains")
public class DnsDomainRuleController {

    private final DnsDomainRuleService service;

    public DnsDomainRuleController(DnsDomainRuleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN', 'VNPT_ENGINEER')")
    public ResponseEntity<ApiResponse<Page<DnsDomainRuleDto>>> getRules(
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DnsDomainRuleDto> result = service.getRules(ruleType, search, pageRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/whitelist")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN', 'VNPT_ENGINEER')")
    public ResponseEntity<ApiResponse<List<DnsDomainRuleDto>>> getActiveWhitelist() {
        return ResponseEntity.ok(ApiResponse.ok(service.getActiveWhitelist()));
    }

    @GetMapping("/blocklist")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN', 'VNPT_ENGINEER')")
    public ResponseEntity<ApiResponse<List<DnsDomainRuleDto>>> getActiveBlocklist() {
        return ResponseEntity.ok(ApiResponse.ok(service.getActiveBlocklist()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN')")
    public ResponseEntity<ApiResponse<DnsDomainRuleDto>> createRule(
            @Valid @RequestBody CreateDnsDomainRuleRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String username = principal != null ? principal.getUsername() : "SYSTEM";
        DnsDomainRuleDto created = service.createRule(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Thêm quy tắc tên miền thành công", created));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN')")
    public ResponseEntity<ApiResponse<DnsDomainRuleDto>> toggleRule(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String username = principal != null ? principal.getUsername() : "SYSTEM";
        DnsDomainRuleDto updated = service.toggleRule(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Chuyển trạng thái quy tắc thành công", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String username = principal != null ? principal.getUsername() : "SYSTEM";
        service.deleteRule(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Xóa quy tắc tên miền thành công", null));
    }
}
