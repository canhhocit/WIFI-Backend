package com.wificaptive.incidents.dto;

public class CitizenReportResponse {

    private Long reportId;
    private boolean incidentCreated;
    private String ticketCode;
    private String locationName;
    private String message;

    public CitizenReportResponse() {}

    public CitizenReportResponse(Long reportId, boolean incidentCreated, String ticketCode, String locationName, String message) {
        this.reportId = reportId;
        this.incidentCreated = incidentCreated;
        this.ticketCode = ticketCode;
        this.locationName = locationName;
        this.message = message;
    }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public boolean isIncidentCreated() { return incidentCreated; }
    public void setIncidentCreated(boolean incidentCreated) { this.incidentCreated = incidentCreated; }

    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
