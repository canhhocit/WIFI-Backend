package com.wificaptive.settings.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.settings.dto.RolePermissionDto;
import com.wificaptive.settings.dto.UpdateRolePermissionRequest;
import com.wificaptive.settings.service.RolePermissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles/permissions")
public class RolePermissionController {

    private final RolePermissionService service;

    public RolePermissionController(RolePermissionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RolePermissionDto>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllPermissions()));
    }

    @GetMapping("/{roleName}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RolePermissionDto>>> getPermissionsByRole(@PathVariable String roleName) {
        return ResponseEntity.ok(ApiResponse.ok(service.getPermissionsByRole(roleName)));
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RolePermissionDto>> updatePermission(
            @Valid @RequestBody UpdateRolePermissionRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String username = principal != null ? principal.getUsername() : "SYSTEM";
        RolePermissionDto updated = service.updatePermission(request, username);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật quyền vai trò thành công", updated));
    }
}
