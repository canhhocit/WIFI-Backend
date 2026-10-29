package com.wificaptive.incidents.dto;

import jakarta.validation.constraints.NotBlank;

public class ResolveIncidentRequest {

    @NotBlank(message = "Biện pháp khắc phục không được để trống")
    private String resolutionNotes;

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}

