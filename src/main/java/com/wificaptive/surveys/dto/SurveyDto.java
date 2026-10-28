package com.wificaptive.surveys.dto;

import com.wificaptive.surveys.entity.Survey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SurveyDto {
    private Long id;
    private Long campaignId;
    private String title;
    private String description;
    private Integer maxResponsesQuota;
    private int currentResponsesCount;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private List<SurveyQuestionDto> questions = new ArrayList<>();

    public static SurveyDto from(Survey s) {
        SurveyDto dto = new SurveyDto();
        dto.setId(s.getId());
        dto.setCampaignId(s.getCampaignId());
        dto.setTitle(s.getTitle());
        dto.setDescription(s.getDescription());
        dto.setMaxResponsesQuota(s.getMaxResponsesQuota());
        dto.setCurrentResponsesCount(s.getCurrentResponsesCount());
        dto.setActive(s.isActive());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getMaxResponsesQuota() { return maxResponsesQuota; }
    public void setMaxResponsesQuota(Integer maxResponsesQuota) { this.maxResponsesQuota = maxResponsesQuota; }
    public int getCurrentResponsesCount() { return currentResponsesCount; }
    public void setCurrentResponsesCount(int currentResponsesCount) { this.currentResponsesCount = currentResponsesCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<SurveyQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<SurveyQuestionDto> questions) { this.questions = questions; }
}

