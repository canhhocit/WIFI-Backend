package com.wificaptive.gateway.dto;

import java.time.Instant;

public class GatewayRevokeResult {
    private final boolean success;
    private final boolean isSimulated;
    private final String message;
    private final Integer statusCode;
    private final long executionTimeMs;
    private final Instant timestamp;

    public GatewayRevokeResult(boolean success, boolean isSimulated, String message, Integer statusCode, long executionTimeMs) {
        this.success = success;
        this.isSimulated = isSimulated;
        this.message = message;
        this.statusCode = statusCode;
        this.executionTimeMs = executionTimeMs;
        this.timestamp = Instant.now();
    }

    public static GatewayRevokeResult success(boolean isSimulated, String message, long executionTimeMs) {
        return new GatewayRevokeResult(true, isSimulated, message, 200, executionTimeMs);
    }

    public static GatewayRevokeResult failure(String errorMessage, Integer statusCode, boolean isSimulated, long executionTimeMs) {
        return new GatewayRevokeResult(false, isSimulated, errorMessage, statusCode, executionTimeMs);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isSimulated() {
        return isSimulated;
    }

    public String getMessage() {
        return message;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
