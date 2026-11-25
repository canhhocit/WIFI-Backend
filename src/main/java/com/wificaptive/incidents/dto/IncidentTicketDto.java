package com.wificaptive.incidents.dto;

import com.wificaptive.incidents.entity.IncidentTicket;
import java.time.Instant;
import java.util.List;

public class IncidentTicketDto {
    private Long id;
    private String ticketCode;
    private Long locationId;
    private String locationName;
    private Long deviceId;
    private String incidentType;
    private String severity;
    private String status;
    private String title;
    private String description;
    private String assignedTo;
    private String assignedGroup;
    private Instant slaDeadline;
    private Instant resolvedAt;
    private String resolutionNotes;
    private String resolutionSummary;
    private String rootCauseCategory;
    private boolean slaBreached;
    private Instant reportedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<IncidentUpdateDto> updates;

    public static IncidentTicketDto from(IncidentTicket t) {
        return from(t, null, null);
    }

    public static IncidentTicketDto from(IncidentTicket t, String locationName, List<IncidentUpdateDto> updates) {
        if (t == null) return null;
        IncidentTicketDto dto = new IncidentTicketDto();
        dto.setId(t.getId());
        dto.setTicketCode(t.getTicketCode());
        dto.setLocationId(t.getLocationId());
        dto.setLocationName(locationName);
        dto.setDeviceId(t.getDeviceId());
        dto.setIncidentType(t.getIncidentType());
        dto.setSeverity(t.getSeverity());
        dto.setStatus(t.getStatus());
        dto.setTitle(t.getTitle());
        dto.setDescription(t.getDescription());
        dto.setAssignedTo(t.getAssignedTo());
        String group = (t.getAssignedGroup() != null && !t.getAssignedGroup().isBlank())
                ? t.getAssignedGroup()
                : (t.getAssignedTo() != null ? "Cán bộ được phân công" : null);
        dto.setAssignedGroup(group);
        dto.setSlaDeadline(t.getSlaDeadline());
        dto.setResolvedAt(t.getResolvedAt());
        dto.setResolutionNotes(t.getResolutionNotes());
        dto.setResolutionSummary(t.getResolutionNotes());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setReportedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        dto.setUpdates(updates);

        Instant now = Instant.now();
        if (t.getResolvedAt() != null) {
            dto.setSlaBreached(t.getResolvedAt().isAfter(t.getSlaDeadline()));
        } else {
            dto.setSlaBreached(t.getSlaDeadline() != null && now.isAfter(t.getSlaDeadline()));
        }

        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getAssignedGroup() { return assignedGroup; }
    public void setAssignedGroup(String assignedGroup) { this.assignedGroup = assignedGroup; }
    public Instant getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(Instant slaDeadline) { this.slaDeadline = slaDeadline; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }
    public String getRootCauseCategory() { return rootCauseCategory; }
    public void setRootCauseCategory(String rootCauseCategory) { this.rootCauseCategory = rootCauseCategory; }
    public boolean isSlaBreached() { return slaBreached; }
    public void setSlaBreached(boolean slaBreached) { this.slaBreached = slaBreached; }
    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<IncidentUpdateDto> getUpdates() { return updates; }
    public void setUpdates(List<IncidentUpdateDto> updates) { this.updates = updates; }
}


