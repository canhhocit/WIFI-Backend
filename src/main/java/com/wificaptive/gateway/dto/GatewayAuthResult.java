package com.wificaptive.gateway.dto;

import java.time.Instant;

public class GatewayAuthResult {
    private final boolean success;
    private final String gatewayReference;
    private final boolean isSimulated;
    private final String message;
    private final Integer statusCode;
    private final long executionTimeMs;
    private final Instant timestamp;

    public GatewayAuthResult(boolean success, String gatewayReference, boolean isSimulated, String message, Integer statusCode, long executionTimeMs) {
        this.success = success;
        this.gatewayReference = gatewayReference;
        this.isSimulated = isSimulated;
        this.message = message;
        this.statusCode = statusCode;
        this.executionTimeMs = executionTimeMs;
        this.timestamp = Instant.now();
    }

    public static GatewayAuthResult success(String gatewayReference, boolean isSimulated, String message, long executionTimeMs) {
        return new GatewayAuthResult(true, gatewayReference, isSimulated, message, 200, executionTimeMs);
    }

    public static GatewayAuthResult failure(String errorMessage, Integer statusCode, boolean isSimulated, long executionTimeMs) {
        return new GatewayAuthResult(false, null, isSimulated, errorMessage, statusCode, executionTimeMs);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getGatewayReference() {
        return gatewayReference;
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
