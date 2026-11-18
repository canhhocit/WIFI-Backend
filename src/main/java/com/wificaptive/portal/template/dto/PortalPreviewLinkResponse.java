package com.wificaptive.portal.template.dto;

import java.time.Instant;

public class PortalPreviewLinkResponse {
    private String token;
    private Instant expiresAt;

    public PortalPreviewLinkResponse() {}

    public PortalPreviewLinkResponse(String token, Instant expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
