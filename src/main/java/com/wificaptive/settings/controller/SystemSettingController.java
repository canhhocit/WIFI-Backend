package com.wificaptive.settings.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.settings.dto.SystemSettingDto;
import com.wificaptive.settings.dto.UpdateSystemSettingRequest;
import com.wificaptive.settings.service.SystemSettingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/system/settings")
public class SystemSettingController {

    private final SystemSettingService service;

    public SystemSettingController(SystemSettingService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN')")
    public ResponseEntity<ApiResponse<List<SystemSettingDto>>> getAllSettings() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllSettings()));
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingDto>> getSettingByKey(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.ok(service.getSettingByKey(key)));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingDto>> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemSettingRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String username = principal != null ? principal.getUsername() : "SYSTEM";
        SystemSettingDto updated = service.updateSetting(key, request, username);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật tham số hệ thống thành công", updated));
    }
}
