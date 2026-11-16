package com.wificaptive.incidents.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateCitizenReportRequest {

    private Long locationId;
    private String locationSlug;

    @NotBlank(message = "Loại phản ánh không được để trống")
    private String issueType; // SLOW_CONNECTION, CANNOT_CONNECT, SIGNAL_WEAK, PORTAL_ERROR, POWER_OUTAGE, OTHER

    private String description;
    private String reporterName;
    private String phoneNumber;
    private String clientMac;
    private String clientIp;

    public CreateCitizenReportRequest() {}

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getLocationSlug() { return locationSlug; }
    public void setLocationSlug(String locationSlug) { this.locationSlug = locationSlug; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getClientMac() { return clientMac; }
    public void setClientMac(String clientMac) { this.clientMac = clientMac; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}
