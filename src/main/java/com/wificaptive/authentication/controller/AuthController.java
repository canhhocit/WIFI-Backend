package com.wificaptive.authentication.controller;

import com.wificaptive.authentication.dto.AdminUserDto;
import com.wificaptive.authentication.dto.LoginRequest;
import com.wificaptive.authentication.dto.LoginResponse;
import com.wificaptive.authentication.service.AuthService;
import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ApiResponse.ok("Authentication successful", response);
    }

    @GetMapping("/me")
    public ApiResponse<AdminUserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        AdminUserDto user = authService.getCurrentUser(principal);
        return ApiResponse.ok(user);
    }
}
