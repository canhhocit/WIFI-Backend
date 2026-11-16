package com.wificaptive.portal.template.dto;

import com.wificaptive.portal.template.entity.PortalTemplateVersion;
import java.time.Instant;

public class PortalTemplateVersionDto {
    private Long id;
    private Long templateId;
    private Integer versionNumber;
    private String versionName;
    private String snapshotJson;
    private String createdBy;
    private Instant createdAt;

    public PortalTemplateVersionDto() {}

    public static PortalTemplateVersionDto from(PortalTemplateVersion entity) {
        PortalTemplateVersionDto dto = new PortalTemplateVersionDto();
        dto.setId(entity.getId());
        dto.setTemplateId(entity.getTemplateId());
        dto.setVersionNumber(entity.getVersionNumber());
        dto.setVersionName(entity.getVersionName());
        dto.setSnapshotJson(entity.getSnapshotJson());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

