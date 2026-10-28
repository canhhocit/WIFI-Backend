package com.wificaptive.audit.dto;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.entity.AuditEvent;

import java.time.Instant;

public class AuditEventDto {
    private Long id;
    private AuditAction action;
    private String actorUsername;
    private String clientIp;
    private String targetType;
    private String targetId;
    private String details;
    private Instant createdAt;

    public AuditEventDto() {
    }

    public static AuditEventDto from(AuditEvent event) {
        AuditEventDto dto = new AuditEventDto();
        dto.setId(event.getId());
        dto.setAction(event.getAction());
        dto.setActorUsername(event.getActorUsername());
        dto.setClientIp(event.getClientIp());
        dto.setTargetType(event.getTargetType());
        dto.setTargetId(event.getTargetId());
        dto.setDetails(event.getDetails());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
