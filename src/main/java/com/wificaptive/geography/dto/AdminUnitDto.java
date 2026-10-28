package com.wificaptive.geography.dto;

import java.time.Instant;
import java.util.List;

public class AdminUnitDto {
    private Long id;
    private Long parentId;
    private String parentName;
    private String code;
    private String name;
    private Integer level;
    private String path;
    private String status;
    private String notes;
    private List<AdminUnitDto> children;
    private Instant createdAt;

    public AdminUnitDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<AdminUnitDto> getChildren() { return children; }
    public void setChildren(List<AdminUnitDto> children) { this.children = children; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

