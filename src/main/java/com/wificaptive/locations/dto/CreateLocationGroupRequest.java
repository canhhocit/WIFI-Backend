package com.wificaptive.locations.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateLocationGroupRequest {

    @NotBlank(message = "Tên nhóm điểm không được để trống")
    private String name;

    @NotBlank(message = "Mã nhóm không được để trống")
    private String code;

    private String description;
    private String color;
    private List<Long> locationIds;

    public CreateLocationGroupRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<Long> getLocationIds() {
        return locationIds;
    }

    public void setLocationIds(List<Long> locationIds) {
        this.locationIds = locationIds;
    }
}
