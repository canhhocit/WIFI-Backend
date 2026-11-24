package com.wificaptive.incidents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateIncidentRequest {

    private Long locationId;

    private Long deviceId;

    private String incidentType = "SYSTEM_DEGRADATION"; // POWER_OUTAGE, FIBER_CUT, HARDWARE_FAULT, SYSTEM_DEGRADATION

    private String severity = "MAJOR"; // CRITICAL, MAJOR, MINOR

    @NotBlank(message = "Tiêu đề sự cố không được để trống")
    private String title;

    private String description;
    private String assignedTo;
    private Integer slaHours; // e.g. 4 for CRITICAL, 8 for MAJOR
    private Long citizenReportId;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getIncidentType() {
        return (incidentType != null && !incidentType.isBlank()) ? incidentType : "SYSTEM_DEGRADATION";
    }
    public void setIncidentType(String incidentType) {
        this.incidentType = (incidentType != null && !incidentType.isBlank()) ? incidentType : "SYSTEM_DEGRADATION";
    }
    public String getSeverity() {
        return (severity != null && !severity.isBlank()) ? severity : "MAJOR";
    }
    public void setSeverity(String severity) {
        this.severity = (severity != null && !severity.isBlank()) ? severity : "MAJOR";
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }
    public Long getCitizenReportId() { return citizenReportId; }
    public void setCitizenReportId(Long citizenReportId) { this.citizenReportId = citizenReportId; }
}

