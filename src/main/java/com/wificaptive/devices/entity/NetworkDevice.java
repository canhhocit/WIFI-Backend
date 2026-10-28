package com.wificaptive.devices.entity;

import com.wificaptive.locations.entity.WifiLocation;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "network_devices")
public class NetworkDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private WifiLocation location;

    @Column(name = "device_type", nullable = false, length = 30)
    private String deviceType; // ONT, ROUTER, ACCESS_POINT

    @Column(name = "serial_number", nullable = false, unique = true, length = 100)
    private String serialNumber;

    @Column(name = "mac_address", nullable = false, unique = true, length = 17)
    private String macAddress;

    @Column(length = 100)
    private String model;

    @Column(length = 100)
    private String vendor;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(nullable = false, length = 20)
    private String status = "ONLINE"; // ONLINE, OFFLINE, DEGRADED, MAINTENANCE

    @Column(name = "cpu_usage_percent")
    private Integer cpuUsagePercent = 0;

    @Column(name = "memory_usage_percent")
    private Integer memoryUsagePercent = 0;

    @Column(name = "uptime_seconds")
    private Long uptimeSeconds = 0L;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "installed_at")
    private Instant installedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public NetworkDevice() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WifiLocation getLocation() { return location; }
    public void setLocation(WifiLocation location) { this.location = location; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(Integer cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }

    public Integer getMemoryUsagePercent() { return memoryUsagePercent; }
    public void setMemoryUsagePercent(Integer memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }

    public Long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(Long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public Instant getInstalledAt() { return installedAt; }
    public void setInstalledAt(Instant installedAt) { this.installedAt = installedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

