package com.wificaptive.reporting.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "government_report_history")
public class GovernmentReportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_code", nullable = false, unique = true, length = 64)
    private String reportCode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 32)
    private String period;

    @Column(name = "total_locations")
    private Integer totalLocations = 1584;

    @Column(name = "online_locations")
    private Integer onlineLocations = 0;

    @Column(name = "sla_percentage")
    private Double slaPercentage = 0.0;

    @Column(name = "total_sessions")
    private Long totalSessions = 0L;

    @Column(name = "total_data_gb")
    private Double totalDataGb = 0.0;

    @Column(name = "export_format", nullable = false, length = 16)
    private String exportFormat = "PDF";

    @Column(nullable = false, length = 32)
    private String status = "SUBMITTED";

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "recipient_agency")
    private String recipientAgency = "UBND TP Hải Phòng & Sở KH&CN";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public GovernmentReportHistory() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (submittedAt == null) submittedAt = Instant.now();
        if (status == null) status = "SUBMITTED";
        if (exportFormat == null) exportFormat = "PDF";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Integer getTotalLocations() { return totalLocations; }
    public void setTotalLocations(Integer totalLocations) { this.totalLocations = totalLocations; }

    public Integer getOnlineLocations() { return onlineLocations; }
    public void setOnlineLocations(Integer onlineLocations) { this.onlineLocations = onlineLocations; }

    public Double getSlaPercentage() { return slaPercentage; }
    public void setSlaPercentage(Double slaPercentage) { this.slaPercentage = slaPercentage; }

    public Long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Long totalSessions) { this.totalSessions = totalSessions; }

    public Double getTotalDataGb() { return totalDataGb; }
    public void setTotalDataGb(Double totalDataGb) { this.totalDataGb = totalDataGb; }

    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public String getRecipientAgency() { return recipientAgency; }
    public void setRecipientAgency(String recipientAgency) { this.recipientAgency = recipientAgency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
