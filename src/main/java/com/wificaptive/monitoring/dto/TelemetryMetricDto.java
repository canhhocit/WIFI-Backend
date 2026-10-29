package com.wificaptive.monitoring.dto;

import com.wificaptive.monitoring.entity.DeviceTelemetryMetric;
import java.time.Instant;

public class TelemetryMetricDto {
    private Long id;
    private Long deviceId;
    private Long locationId;
    private Double cpuUsagePercent;
    private Double memoryUsagePercent;
    private int activeClientCount;
    private Long rxBytesSec;
    private Long txBytesSec;
    private int pingLatencyMs;
    private Double packetLossPercent;
    private Instant recordedAt;

    public static TelemetryMetricDto from(DeviceTelemetryMetric m) {
        TelemetryMetricDto dto = new TelemetryMetricDto();
        dto.setId(m.getId());
        dto.setDeviceId(m.getDeviceId());
        dto.setLocationId(m.getLocationId());
        dto.setCpuUsagePercent(m.getCpuUsagePercent());
        dto.setMemoryUsagePercent(m.getMemoryUsagePercent());
        dto.setActiveClientCount(m.getActiveClientCount());
        dto.setRxBytesSec(m.getRxBytesSec());
        dto.setTxBytesSec(m.getTxBytesSec());
        dto.setPingLatencyMs(m.getPingLatencyMs());
        dto.setPacketLossPercent(m.getPacketLossPercent());
        dto.setRecordedAt(m.getRecordedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}

