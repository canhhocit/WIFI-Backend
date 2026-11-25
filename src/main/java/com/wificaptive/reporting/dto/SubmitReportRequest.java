package com.wificaptive.reporting.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class SubmitReportRequest {
    @NotBlank(message = "Kỳ báo cáo không được để trống")
    @JsonAlias("reportPeriod")
    private String period;

    private String title;
    private String recipientAgency;
    private String notes;
    private String exportFormat;

    public SubmitReportRequest() {}

    public SubmitReportRequest(String period, String recipientAgency, String notes, String exportFormat) {
        this(period, null, recipientAgency, notes, exportFormat);
    }

    public SubmitReportRequest(String period, String title, String recipientAgency, String notes, String exportFormat) {
        this.period = period;
        this.title = title;
        this.recipientAgency = recipientAgency;
        this.notes = notes;
        this.exportFormat = exportFormat;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRecipientAgency() { return recipientAgency; }
    public void setRecipientAgency(String recipientAgency) { this.recipientAgency = recipientAgency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
}
