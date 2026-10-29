package com.wificaptive.settings.dto;

import com.wificaptive.settings.entity.RolePermission;

public class RolePermissionDto {
    private Long id;
    private String roleName;
    private String moduleName;
    private boolean canView;
    private boolean canCreate;
    private boolean canEdit;
    private boolean canDelete;
    private boolean canApprove;

    public RolePermissionDto() {
    }

    public static RolePermissionDto from(RolePermission entity) {
        RolePermissionDto dto = new RolePermissionDto();
        dto.setId(entity.getId());
        dto.setRoleName(entity.getRoleName());
        dto.setModuleName(entity.getModuleName());
        dto.setCanView(entity.isCanView());
        dto.setCanCreate(entity.isCanCreate());
        dto.setCanEdit(entity.isCanEdit());
        dto.setCanDelete(entity.isCanDelete());
        dto.setCanApprove(entity.isCanApprove());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public boolean isCanView() {
        return canView;
    }

    public void setCanView(boolean canView) {
        this.canView = canView;
    }

    public boolean isCanCreate() {
        return canCreate;
    }

    public void setCanCreate(boolean canCreate) {
        this.canCreate = canCreate;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public boolean isCanApprove() {
        return canApprove;
    }

    public void setCanApprove(boolean canApprove) {
        this.canApprove = canApprove;
    }
}
