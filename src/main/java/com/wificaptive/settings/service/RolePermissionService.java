package com.wificaptive.settings.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.settings.dto.RolePermissionDto;
import com.wificaptive.settings.dto.UpdateRolePermissionRequest;
import com.wificaptive.settings.entity.RolePermission;
import com.wificaptive.settings.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
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
}
