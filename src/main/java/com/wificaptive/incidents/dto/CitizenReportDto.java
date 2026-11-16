package com.wificaptive.incidents.dto;

import com.wificaptive.incidents.entity.CitizenIssueReport;
import java.time.Instant;

public class CitizenReportDto {

    private Long id;
    private Long locationId;
    private String locationName;
    private String clientMac;
    private String clientIp;
    private String phoneNumber;
    private String reporterName;
    private String issueType;
    private String description;
    private String status;
    private Long incidentTicketId;
    private String ticketCode;
    private Instant createdAt;

    public static CitizenReportDto from(CitizenIssueReport r, String locationName, String ticketCode) {
        CitizenReportDto dto = new CitizenReportDto();
        dto.setId(r.getId());
        dto.setLocationId(r.getLocationId());
        dto.setLocationName(locationName);
        dto.setClientMac(r.getClientMac());
        dto.setClientIp(r.getClientIp());
        dto.setPhoneNumber(r.getPhoneNumber());
        dto.setReporterName(r.getReporterName());
        dto.setIssueType(r.getIssueType());
        dto.setDescription(r.getDescription());
        dto.setStatus(r.getStatus());
        dto.setIncidentTicketId(r.getIncidentTicketId());
        dto.setTicketCode(ticketCode);
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getClientMac() { return clientMac; }
    public void setClientMac(String clientMac) { this.clientMac = clientMac; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getIncidentTicketId() { return incidentTicketId; }
    public void setIncidentTicketId(Long incidentTicketId) { this.incidentTicketId = incidentTicketId; }

    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
