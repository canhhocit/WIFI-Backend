package com.wificaptive.monitoring.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alert_rules")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType; // OFFLINE, HIGH_CPU, HIGH_PACKET_LOSS, BANDWIDTH_EXHAUSTION

    @Column(name = "threshold_value", nullable = false)
    private Double thresholdValue;

    @Column(nullable = false, length = 20)
    private String severity = "MAJOR"; // CRITICAL, MAJOR, MINOR, WARNING

    @Column(name = "notification_channel", nullable = false, length = 50)
    private String notificationChannel = "TELEGRAM"; // TELEGRAM, WEBHOOK, EMAIL, SMS

    @Column(name = "target_channel_id")
    private String targetChannelId;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AlertRule() {}

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

