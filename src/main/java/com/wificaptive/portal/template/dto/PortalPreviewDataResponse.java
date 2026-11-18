package com.wificaptive.portal.template.dto;

import com.wificaptive.portal.dto.PortalInitResponse;

import java.time.Instant;

public class PortalPreviewDataResponse {
    private PortalInitResponse previewData;
    private Instant expiresAt;

    public PortalPreviewDataResponse() {}

    public PortalPreviewDataResponse(PortalInitResponse previewData, Instant expiresAt) {
        this.previewData = previewData;
        this.expiresAt = expiresAt;
    }

    public PortalInitResponse getPreviewData() { return previewData; }
    public void setPreviewData(PortalInitResponse previewData) { this.previewData = previewData; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
