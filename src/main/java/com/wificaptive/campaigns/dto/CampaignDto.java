package com.wificaptive.campaigns.dto;

import com.wificaptive.campaigns.entity.Campaign;
import java.time.Instant;

public class CampaignDto {
    private Long id;
    private String title;
    private String description;
    private String bannerUrl;
    private String videoUrl;
    private String actionUrl;
    private Long targetAdminUnitId;
    private Long targetLocationId;
    private int priority;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private String createdBy;
    private String approvedBy;
    private Instant approvedAt;
    private String rejectReason;
    private Instant createdAt;
    private Instant updatedAt;

    public static CampaignDto from(Campaign c) {
        CampaignDto dto = new CampaignDto();
        dto.setId(c.getId());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setBannerUrl(c.getBannerUrl());
        dto.setVideoUrl(c.getVideoUrl());
        dto.setActionUrl(c.getActionUrl());
        dto.setTargetAdminUnitId(c.getTargetAdminUnitId());
        dto.setTargetLocationId(c.getTargetLocationId());
        dto.setPriority(c.getPriority());
        dto.setStartTime(c.getStartTime());
        dto.setEndTime(c.getEndTime());
        dto.setStatus(c.getStatus());
        dto.setCreatedBy(c.getCreatedBy());
        dto.setApprovedBy(c.getApprovedBy());
        dto.setApprovedAt(c.getApprovedAt());
        dto.setRejectReason(c.getRejectReason());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public Long getTargetAdminUnitId() { return targetAdminUnitId; }
    public void setTargetAdminUnitId(Long targetAdminUnitId) { this.targetAdminUnitId = targetAdminUnitId; }
    public Long getTargetLocationId() { return targetLocationId; }
    public void setTargetLocationId(Long targetLocationId) { this.targetLocationId = targetLocationId; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

