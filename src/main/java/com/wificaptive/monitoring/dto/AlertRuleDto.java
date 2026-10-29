package com.wificaptive.monitoring.dto;

import com.wificaptive.monitoring.entity.AlertRule;

public class AlertRuleDto {
    private Long id;
    private String ruleName;
    private String metricType;
    private Double thresholdValue;
    private String severity;
    private String notificationChannel;
    private String targetChannelId;
    private boolean enabled;

    public static AlertRuleDto from(AlertRule r) {
        AlertRuleDto dto = new AlertRuleDto();
        dto.setId(r.getId());
        dto.setRuleName(r.getRuleName());
        dto.setMetricType(r.getMetricType());
        dto.setThresholdValue(r.getThresholdValue());
        dto.setSeverity(r.getSeverity());
        dto.setNotificationChannel(r.getNotificationChannel());
        dto.setTargetChannelId(r.getTargetChannelId());
        dto.setEnabled(r.isEnabled());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getNotificationChannel() { return notificationChannel; }
    public void setNotificationChannel(String notificationChannel) { this.notificationChannel = notificationChannel; }
    public String getTargetChannelId() { return targetChannelId; }
    public void setTargetChannelId(String targetChannelId) { this.targetChannelId = targetChannelId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}

