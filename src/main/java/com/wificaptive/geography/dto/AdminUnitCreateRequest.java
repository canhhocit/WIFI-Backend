package com.wificaptive.geography.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminUnitCreateRequest {

    private Long parentId;

    @NotBlank(message = "Mã đơn vị địa bàn không được để trống")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Tên đơn vị địa bàn không được để trống")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Cấp địa bàn không được để trống")
    private Integer level;

    private String notes;

    public AdminUnitCreateRequest() {}

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

