package com.wificaptive.surveys.dto;

import com.wificaptive.surveys.entity.SurveyResponse;
import java.time.Instant;

public class SurveyResponseDto {
    private Long id;
    private Long surveyId;
    private Long locationId;
    private String clientMac;
    private String answersJson;
    private Instant submittedAt;

    public static SurveyResponseDto from(SurveyResponse r) {
        SurveyResponseDto dto = new SurveyResponseDto();
        dto.setId(r.getId());
        dto.setSurveyId(r.getSurveyId());
        dto.setLocationId(r.getLocationId());
        dto.setClientMac(r.getClientMac());
        dto.setAnswersJson(r.getAnswersJson());
        dto.setSubmittedAt(r.getSubmittedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public String getClientMac() { return clientMac; }
    public void setClientMac(String clientMac) { this.clientMac = clientMac; }
    public String getAnswersJson() { return answersJson; }
    public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}

