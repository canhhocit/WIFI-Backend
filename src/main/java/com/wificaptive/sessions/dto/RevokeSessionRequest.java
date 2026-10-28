package com.wificaptive.sessions.dto;

import jakarta.validation.constraints.NotBlank;

public class RevokeSessionRequest {

    @NotBlank(message = "Revocation reason is required")
    private String reason;

    public RevokeSessionRequest() {
    }

    public RevokeSessionRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
