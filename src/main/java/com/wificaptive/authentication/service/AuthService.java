package com.wificaptive.authentication.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.authentication.dto.AdminUserDto;
import com.wificaptive.authentication.dto.LoginRequest;
import com.wificaptive.authentication.dto.LoginResponse;
import com.wificaptive.authentication.entity.AdminUser;
import com.wificaptive.authentication.repository.AdminUserRepository;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.common.security.JwtTokenProvider;
import com.wificaptive.common.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AdminUserRepository adminUserRepository;
    private final AuditService auditService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            AdminUserRepository adminUserRepository,
            AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.adminUserRepository = adminUserRepository;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        AdminUser user = adminUserRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "id", userPrincipal.getId()));

        String clientIp = extractClientIp(request);
        auditService.recordEvent(
                AuditAction.ADMIN_LOGIN,
                user.getUsername(),
                clientIp,
                "USER",
                user.getId().toString(),
                "Administrator successfully authenticated from IP: " + clientIp
        );

        return new LoginResponse(token, AdminUserDto.from(user));
    }

    public AdminUserDto getCurrentUser(UserPrincipal principal) {
        AdminUser user = adminUserRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "id", principal.getId()));
        return AdminUserDto.from(user);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
