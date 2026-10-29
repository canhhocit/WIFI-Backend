package com.wificaptive.monitoring.dto;

import jakarta.validation.constraints.NotNull;

public class TelemetryIngestRequest {

    @NotNull(message = "ID thiết bị không được để trống")
    private Long deviceId;

    private Long locationId;
    private Double cpuUsagePercent;
    private Double memoryUsagePercent;
    private int activeClientCount;
    private Long rxBytesSec;
    private Long txBytesSec;
    private int pingLatencyMs;
    private Double packetLossPercent;

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(Double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
    public Double getMemoryUsagePercent() { return memoryUsagePercent; }
    public void setMemoryUsagePercent(Double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }
    public int getActiveClientCount() { return activeClientCount; }
    public void setActiveClientCount(int activeClientCount) { this.activeClientCount = activeClientCount; }
    public Long getRxBytesSec() { return rxBytesSec; }
    public void setRxBytesSec(Long rxBytesSec) { this.rxBytesSec = rxBytesSec; }
    public Long getTxBytesSec() { return txBytesSec; }
    public void setTxBytesSec(Long txBytesSec) { this.txBytesSec = txBytesSec; }
    public int getPingLatencyMs() { return pingLatencyMs; }
    public void setPingLatencyMs(int pingLatencyMs) { this.pingLatencyMs = pingLatencyMs; }
    public Double getPacketLossPercent() { return packetLossPercent; }
    public void setPacketLossPercent(Double packetLossPercent) { this.packetLossPercent = packetLossPercent; }
}

