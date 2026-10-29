package com.wificaptive.settings.dto;

import com.wificaptive.settings.entity.SystemSetting;
import java.time.Instant;

public class SystemSettingDto {
    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private String category;
    private Instant updatedAt;

    public SystemSettingDto() {
    }

    public static SystemSettingDto from(SystemSetting entity) {
        SystemSettingDto dto = new SystemSettingDto();
        dto.setId(entity.getId());
        dto.setSettingKey(entity.getSettingKey());
        dto.setSettingValue(entity.getSettingValue());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

