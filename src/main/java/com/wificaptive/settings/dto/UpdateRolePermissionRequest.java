package com.wificaptive.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateRolePermissionRequest {

    @NotBlank(message = "Tên vai trò không được để trống")
    private String roleName;

    @NotBlank(message = "Tên phân hệ không được để trống")
    private String moduleName;

    @NotNull
    private Boolean canView;

    @NotNull
    private Boolean canCreate;

    @NotNull
    private Boolean canEdit;

    @NotNull
    private Boolean canDelete;

    @NotNull
    private Boolean canApprove;

    public UpdateRolePermissionRequest() {
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

    public Boolean getCanView() {
        return canView;
    }

    public void setCanView(Boolean canView) {
        this.canView = canView;
    }

    public Boolean getCanCreate() {
        return canCreate;
    }

    public void setCanCreate(Boolean canCreate) {
        this.canCreate = canCreate;
    }

    public Boolean getCanEdit() {
        return canEdit;
    }

    public void setCanEdit(Boolean canEdit) {
        this.canEdit = canEdit;
    }

    public Boolean getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }

    public Boolean getCanApprove() {
        return canApprove;
    }

    public void setCanApprove(Boolean canApprove) {
        this.canApprove = canApprove;
    }
}

