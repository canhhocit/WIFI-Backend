package com.wificaptive.settings.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "role_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role_name", "module_name"})
})
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "module_name", nullable = false, length = 50)
    private String moduleName;

    @Column(name = "can_view", nullable = false)
    private boolean canView = true;

    @Column(name = "can_create", nullable = false)
    private boolean canCreate = false;

    @Column(name = "can_edit", nullable = false)
    private boolean canEdit = false;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = false;

    @Column(name = "can_approve", nullable = false)
    private boolean canApprove = false;

    public RolePermission() {
    }

    public RolePermission(String roleName, String moduleName, boolean canView, boolean canCreate, boolean canEdit, boolean canDelete, boolean canApprove) {
        this.roleName = roleName;
        this.moduleName = moduleName;
        this.canView = canView;
        this.canCreate = canCreate;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
        this.canApprove = canApprove;
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

