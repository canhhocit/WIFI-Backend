package com.wificaptive.settings.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.settings.dto.RolePermissionDto;
import com.wificaptive.settings.dto.UpdateRolePermissionRequest;
import com.wificaptive.settings.entity.RolePermission;
import com.wificaptive.settings.repository.RolePermissionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service("rbac")
public class RolePermissionService {

    private final RolePermissionRepository repository;
    private final AuditService auditService;

    public RolePermissionService(RolePermissionRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RolePermissionDto> getAllPermissions() {
        return repository.findAll().stream()
                .map(RolePermissionDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RolePermissionDto> getPermissionsByRole(String roleName) {
        return repository.findByRoleName(roleName).stream()
                .map(RolePermissionDto::from)
                .toList();
    }

    @Transactional
    public RolePermissionDto updatePermission(UpdateRolePermissionRequest request, String currentUsername) {
        RolePermission perm = repository.findByRoleNameAndModuleName(request.getRoleName(), request.getModuleName())
                .orElseGet(() -> {
                    RolePermission newPerm = new RolePermission();
                    newPerm.setRoleName(request.getRoleName());
                    newPerm.setModuleName(request.getModuleName());
                    return newPerm;
                });

        perm.setCanView(request.getCanView());
        perm.setCanCreate(request.getCanCreate());
        perm.setCanEdit(request.getCanEdit());
        perm.setCanDelete(request.getCanDelete());
        perm.setCanApprove(request.getCanApprove());

        RolePermission saved = repository.save(perm);

        auditService.recordEvent(
                AuditAction.ROLE_PERMISSION_UPDATE,
                currentUsername,
                "SYSTEM",
                "ROLE_PERMISSION",
                saved.getId().toString(),
                "Cập nhật ma trận phân quyền cho vai trò: " + saved.getRoleName() + " trên phân hệ: " + saved.getModuleName()
        );

        return RolePermissionDto.from(saved);
    }

    @Transactional
    public List<RolePermissionDto> updatePermissionsBatch(List<UpdateRolePermissionRequest> requests, String currentUsername) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(req -> updatePermission(req, currentUsername))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(String moduleName, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        for (GrantedAuthority authority : auth.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_SUPER_ADMIN".equals(role) || "ROLE_ADMIN".equals(role)) {
                return true;
            }
        }

        for (GrantedAuthority authority : auth.getAuthorities()) {
            String role = authority.getAuthority();
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }

            Optional<RolePermission> permOpt = repository.findByRoleNameAndModuleName(role, moduleName);
            if (permOpt.isEmpty() && "ROLE_DISTRICT_OFFICER".equals(role)) {
                permOpt = repository.findByRoleNameAndModuleName("ROLE_COMMUNE_OFFICER", moduleName);
            }

            if (permOpt.isPresent()) {
                RolePermission perm = permOpt.get();
                boolean allowed = switch (action.toUpperCase()) {
                    case "VIEW" -> perm.isCanView();
                    case "CREATE" -> perm.isCanCreate();
                    case "EDIT" -> perm.isCanEdit();
                    case "DELETE" -> perm.isCanDelete();
                    case "APPROVE" -> perm.isCanApprove();
                    default -> false;
                };
                if (allowed) {
                    return true;
                }
            }
        }

        return false;
    }
}

