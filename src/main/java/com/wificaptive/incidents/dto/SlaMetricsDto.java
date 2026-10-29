package com.wificaptive.incidents.dto;

public class SlaMetricsDto {
    private long totalTickets;
    private long openTickets;
    private long resolvedTickets;
    private long breachedTickets;
    private double slaCompliancePercentage;
    private double mttrHours; // Mean Time To Resolution in hours

    public long getTotalTickets() { return totalTickets; }
    public void setTotalTickets(long totalTickets) { this.totalTickets = totalTickets; }
    public long getOpenTickets() { return openTickets; }
    public void setOpenTickets(long openTickets) { this.openTickets = openTickets; }
    public long getResolvedTickets() { return resolvedTickets; }
    public void setResolvedTickets(long resolvedTickets) { this.resolvedTickets = resolvedTickets; }
    public long getBreachedTickets() { return breachedTickets; }
    public void setBreachedTickets(long breachedTickets) { this.breachedTickets = breachedTickets; }
    public double getSlaCompliancePercentage() { return slaCompliancePercentage; }
    public void setSlaCompliancePercentage(double slaCompliancePercentage) { this.slaCompliancePercentage = slaCompliancePercentage; }
    public double getMttrHours() { return mttrHours; }
    public void setMttrHours(double mttrHours) { this.mttrHours = mttrHours; }
}

