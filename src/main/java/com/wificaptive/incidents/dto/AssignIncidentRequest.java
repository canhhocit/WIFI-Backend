package com.wificaptive.incidents.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignIncidentRequest {

    @NotBlank(message = "Người nhận phân công không được để trống")
    private String assignedTo;

    private Integer additionalHours;
    private String notes;

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public Integer getAdditionalHours() { return additionalHours; }
    public void setAdditionalHours(Integer additionalHours) { this.additionalHours = additionalHours; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

