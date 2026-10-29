package com.wificaptive.monitoring.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "device_telemetry_metrics")
public class DeviceTelemetryMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    @Column(name = "memory_usage_percent")
    private Double memoryUsagePercent;

    @Column(name = "active_client_count")
    private int activeClientCount = 0;

    @Column(name = "rx_bytes_sec")
    private Long rxBytesSec = 0L;

    @Column(name = "tx_bytes_sec")
    private Long txBytesSec = 0L;

    @Column(name = "ping_latency_ms")
    private int pingLatencyMs = 0;

    @Column(name = "packet_loss_percent")
    private Double packetLossPercent = 0.0;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt = Instant.now();

    public DeviceTelemetryMetric() {}

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

