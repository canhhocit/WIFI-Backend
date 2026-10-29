package com.wificaptive.authentication.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.authentication.dto.AdminUserDto;
import com.wificaptive.authentication.dto.CreateAdminUserRequest;
import com.wificaptive.authentication.dto.UpdateAdminUserRequest;
import com.wificaptive.authentication.entity.AdminRole;
import com.wificaptive.authentication.entity.AdminUser;
import com.wificaptive.authentication.repository.AdminUserRepository;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final AdministrativeUnitRepository administrativeUnitRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminUserService(
            AdminUserRepository adminUserRepository,
            AdministrativeUnitRepository administrativeUnitRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.adminUserRepository = adminUserRepository;
        this.administrativeUnitRepository = administrativeUnitRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> getUsers(String search, Pageable pageable) {
        Page<AdminUser> page;
        if (search != null && !search.trim().isEmpty()) {
            page = adminUserRepository.findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(search.trim(), search.trim(), pageable);
        } else {
            page = adminUserRepository.findAll(pageable);
        }

        return page.map(this::enrichDto);
    }

    @Transactional(readOnly = true)
    public AdminUserDto getUserById(Long id) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "id", id));
        return enrichDto(user);
    }

    @Transactional
    public AdminUserDto createUser(CreateAdminUserRequest request, String currentUsername) {
        if (adminUserRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập '" + request.getUsername() + "' đã tồn tại");
        }
        if (adminUserRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' đã được sử dụng");
        }

        AdminUser user = new AdminUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setAdminUnitId(request.getAdminUnitId());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEnabled(true);

        AdminUser saved = adminUserRepository.save(user);

        auditService.recordEvent(
                AuditAction.ADMIN_CREATE_USER,
                currentUsername,
                "SYSTEM",
                "ADMIN_USER",
                saved.getId().toString(),
                "Tạo tài khoản cán bộ mới: " + saved.getUsername() + " (Vai trò: " + saved.getRole() + ")"
        );

        return enrichDto(saved);
    }

    @Transactional
    public AdminUserDto updateUser(Long id, UpdateAdminUserRequest request, String currentUsername) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "id", id));

        // Check if email changed and taken
        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && adminUserRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' đã được sử dụng bởi người khác");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setAdminUnitId(request.getAdminUnitId());
        user.setPhoneNumber(request.getPhoneNumber());

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        }

        AdminUser updated = adminUserRepository.save(user);

        auditService.recordEvent(
                AuditAction.ADMIN_UPDATE_USER,
                currentUsername,
                "SYSTEM",
                "ADMIN_USER",
                updated.getId().toString(),
                "Cập nhật thông tin cán bộ: " + updated.getUsername()
        );

        return enrichDto(updated);
    }

    @Transactional
    public AdminUserDto toggleUserStatus(Long id, String currentUsername) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "id", id));

        user.setEnabled(!user.isEnabled());
        AdminUser updated = adminUserRepository.save(user);

        auditService.recordEvent(
                AuditAction.ADMIN_TOGGLE_USER_STATUS,
                currentUsername,
                "SYSTEM",
                "ADMIN_USER",
                updated.getId().toString(),
                "Chuyển trạng thái tài khoản: " + updated.getUsername() + " -> " + (updated.isEnabled() ? "Kích hoạt" : "Vô hiệu hóa")
        );

        return enrichDto(updated);
    }

    @Transactional
    public AdminUserDto unlockUser(Long id, String currentUsername) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "id", id));

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        AdminUser updated = adminUserRepository.save(user);

        auditService.recordEvent(
                AuditAction.ADMIN_UNLOCK_USER,
                currentUsername,
                "SYSTEM",
                "ADMIN_USER",
                updated.getId().toString(),
                "Mở khóa tài khoản cán bộ: " + updated.getUsername()
        );

        return enrichDto(updated);
    }

    @Transactional
    public void deleteUser(Long id, String currentUsername) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "id", id));

        if (user.getRole() == AdminRole.ROLE_SUPER_ADMIN) {
            throw new BadRequestException("Không thể xóa tài khoản Quản trị cấp cao (SUPER_ADMIN)");
        }

        adminUserRepository.delete(user);

        auditService.recordEvent(
                AuditAction.ADMIN_DELETE_USER,
                currentUsername,
                "SYSTEM",
                "ADMIN_USER",
                id.toString(),
                "Xóa tài khoản cán bộ: " + user.getUsername()
        );
    }

    private AdminUserDto enrichDto(AdminUser user) {
        AdminUserDto dto = AdminUserDto.from(user);
        if (user.getAdminUnitId() != null) {
            administrativeUnitRepository.findById(user.getAdminUnitId())
                    .ifPresent(unit -> dto.setAdminUnitName(unit.getName() + " (" + unit.getLevel() + ")"));
        }
        return dto;
    }
}
