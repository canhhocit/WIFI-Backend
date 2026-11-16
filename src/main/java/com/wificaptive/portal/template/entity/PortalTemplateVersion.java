package com.wificaptive.portal.template.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "portal_template_versions")
public class PortalTemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "version_name", length = 255)
    private String versionName;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_by", length = 100)
    private String createdBy = "admin";

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public PortalTemplateVersion() {}

    public PortalTemplateVersion(Long templateId, Integer versionNumber, String versionName, String snapshotJson, String createdBy) {
        this.templateId = templateId;
        this.versionNumber = versionNumber;
        this.versionName = versionName;
        this.snapshotJson = snapshotJson;
        this.createdBy = createdBy != null ? createdBy : "admin";
        this.createdAt = Instant.now();
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

