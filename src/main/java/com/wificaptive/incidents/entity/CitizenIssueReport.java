package com.wificaptive.incidents.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "citizen_issue_reports")
public class CitizenIssueReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "client_mac", length = 50)
    private String clientMac;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "reporter_name", length = 100)
    private String reporterName;

    @Column(name = "issue_type", nullable = false, length = 50)
    private String issueType; // SLOW_CONNECTION, CANNOT_CONNECT, SIGNAL_WEAK, PORTAL_ERROR, POWER_OUTAGE, OTHER

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private String status = "PENDING"; // PENDING, AGGREGATED_TO_INCIDENT, RESOLVED

    @Column(name = "incident_ticket_id")
    private Long incidentTicketId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CitizenIssueReport() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
