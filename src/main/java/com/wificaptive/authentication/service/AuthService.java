package com.wificaptive.authentication.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.authentication.dto.AdminUserDto;
import com.wificaptive.authentication.dto.ChangePasswordRequest;
import com.wificaptive.authentication.dto.LoginRequest;
import com.wificaptive.authentication.dto.LoginResponse;
import com.wificaptive.authentication.entity.AdminUser;
import com.wificaptive.authentication.repository.AdminUserRepository;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.common.security.JwtTokenProvider;
import com.wificaptive.common.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AdminUserRepository adminUserRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            AdminUserRepository adminUserRepository,
            AuditService auditService,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.adminUserRepository = adminUserRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
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

    public void changePassword(Long userId, ChangePasswordRequest request, HttpServletRequest httpRequest) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Xác nhận mật khẩu mới không khớp");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminUserRepository.save(user);

        String clientIp = extractClientIp(httpRequest);
        auditService.recordEvent(
                AuditAction.ADMIN_UPDATE_USER,
                user.getUsername(),
                clientIp,
                "USER",
                user.getId().toString(),
                "Người dùng đã thay đổi mật khẩu thành công từ IP: " + clientIp
        );
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
