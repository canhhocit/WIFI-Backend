package com.wificaptive.portal.template.dto;

import com.wificaptive.portal.dto.PortalInitResponse;

public class PortalPreviewLinkRequest {
    private PortalInitResponse previewData;
    private Integer expiresInMinutes = 60;

    public PortalInitResponse getPreviewData() { return previewData; }
    public void setPreviewData(PortalInitResponse previewData) { this.previewData = previewData; }
    public Integer getExpiresInMinutes() { return expiresInMinutes; }
    public void setExpiresInMinutes(Integer expiresInMinutes) { this.expiresInMinutes = expiresInMinutes; }
}
