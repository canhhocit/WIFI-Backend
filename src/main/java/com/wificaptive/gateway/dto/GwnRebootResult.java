package com.wificaptive.gateway.dto;

public class GwnRebootResult {
    private boolean success;
    private String message;
    private String macAddress;
    private boolean simulated;
    private long latencyMs;

    public GwnRebootResult() {}

    public GwnRebootResult(boolean success, String message, String macAddress, boolean simulated, long latencyMs) {
        this.success = success;
        this.message = message;
        this.macAddress = macAddress;
        this.simulated = simulated;
        this.latencyMs = latencyMs;
    }

    public static GwnRebootResult success(String message, String macAddress, boolean simulated, long latencyMs) {
        return new GwnRebootResult(true, message, macAddress, simulated, latencyMs);
    }

    public static GwnRebootResult failure(String message, String macAddress, boolean simulated, long latencyMs) {
        return new GwnRebootResult(false, message, macAddress, simulated, latencyMs);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}

