package com.wificaptive.settings.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateSystemSettingRequest {

    @NotBlank(message = "Giá trị cấu hình không được để trống")
    private String settingValue;

    private String description;

    public UpdateSystemSettingRequest() {
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
}
