package com.wificaptive.surveys.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class SurveyResponseSubmitRequest {

    private Long locationId;

    @NotBlank(message = "Địa chỉ MAC không được để trống")
    private String clientMac;

    private Map<String, Object> answers;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getClientMac() { return clientMac; }
    public void setClientMac(String clientMac) { this.clientMac = clientMac; }

    public Map<String, Object> getAnswers() { return answers; }
    public void setAnswers(Map<String, Object> answers) { this.answers = answers; }
}

