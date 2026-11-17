package com.wificaptive.reporting.dto;

import jakarta.validation.constraints.NotBlank;

public class SubmitReportRequest {
    @NotBlank(message = "Kỳ báo cáo không được để trống")
    private String period;

    private String recipientAgency;
    private String notes;
    private String exportFormat;

    public SubmitReportRequest() {}

    public SubmitReportRequest(String period, String recipientAgency, String notes, String exportFormat) {
        this.period = period;
        this.recipientAgency = recipientAgency;
        this.notes = notes;
        this.exportFormat = exportFormat;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getRecipientAgency() { return recipientAgency; }
    public void setRecipientAgency(String recipientAgency) { this.recipientAgency = recipientAgency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
}
