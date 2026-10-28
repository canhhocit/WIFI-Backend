package com.wificaptive.authentication.dto;

import com.wificaptive.authentication.entity.AdminRole;
import com.wificaptive.authentication.entity.AdminUser;

import java.time.Instant;

public class AdminUserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private AdminRole role;
    private boolean enabled;
    private Instant createdAt;

    public AdminUserDto() {
    }

    public static AdminUserDto from(AdminUser user) {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
