package com.wificaptive.gateway.dto;

import java.time.Instant;

public class GatewayStatusDto {
    private String adapterType;
    private boolean healthy;
    private boolean isSimulated;
    private String version;
    private String statusMessage;
    private long totalAuthorizations;
    private long totalRevocations;
    private long failedOperations;
    private boolean simulateFailures;
    private int simulatedLatencyMs;
    private Instant lastCheckedAt;

    public GatewayStatusDto() {
        this.lastCheckedAt = Instant.now();
    }

    public String getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(String adapterType) {
        this.adapterType = adapterType;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public boolean isSimulated() {
        return isSimulated;
    }

    public void setSimulated(boolean simulated) {
        isSimulated = simulated;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public long getTotalAuthorizations() {
        return totalAuthorizations;
    }

    public void setTotalAuthorizations(long totalAuthorizations) {
        this.totalAuthorizations = totalAuthorizations;
    }

    public long getTotalRevocations() {
        return totalRevocations;
    }

    public void setTotalRevocations(long totalRevocations) {
        this.totalRevocations = totalRevocations;
    }

    public long getFailedOperations() {
        return failedOperations;
    }

    public void setFailedOperations(long failedOperations) {
        this.failedOperations = failedOperations;
    }

    public boolean isSimulateFailures() {
        return simulateFailures;
    }

    public void setSimulateFailures(boolean simulateFailures) {
        this.simulateFailures = simulateFailures;
    }

    public int getSimulatedLatencyMs() {
        return simulatedLatencyMs;
    }

    public void setSimulatedLatencyMs(int simulatedLatencyMs) {
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}
