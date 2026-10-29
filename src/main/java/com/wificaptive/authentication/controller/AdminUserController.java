package com.wificaptive.authentication.controller;

import com.wificaptive.authentication.dto.AdminUserDto;
import com.wificaptive.authentication.dto.CreateAdminUserRequest;
import com.wificaptive.authentication.dto.UpdateAdminUserRequest;
import com.wificaptive.authentication.service.AdminUserService;
import com.wificaptive.common.dto.ApiResponse;
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

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN')")
    public ResponseEntity<ApiResponse<Page<AdminUserDto>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminUserDto> users = adminUserService.getUsers(search, pageRequest);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDto>> getUserById(@PathVariable Long id) {
        AdminUserDto user = adminUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDto>> createUser(
            @Valid @RequestBody CreateAdminUserRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String username = principal != null ? principal.getUsername() : "SYSTEM";
        AdminUserDto created = adminUserService.createUser(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Tạo tài khoản thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminUserRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String username = principal != null ? principal.getUsername() : "SYSTEM";
        AdminUserDto updated = adminUserService.updateUser(id, request, username);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật tài khoản thành công", updated));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDto>> toggleUserStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String username = principal != null ? principal.getUsername() : "SYSTEM";
        AdminUserDto updated = adminUserService.toggleUserStatus(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Chuyển trạng thái tài khoản thành công", updated));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDto>> unlockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String username = principal != null ? principal.getUsername() : "SYSTEM";
        AdminUserDto updated = adminUserService.unlockUser(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Mở khóa tài khoản thành công", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String username = principal != null ? principal.getUsername() : "SYSTEM";
        adminUserService.deleteUser(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Xóa tài khoản thành công", null));
    }
}
