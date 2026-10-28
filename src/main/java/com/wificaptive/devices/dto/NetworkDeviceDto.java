package com.wificaptive.devices.dto;

import com.wificaptive.devices.entity.NetworkDevice;
import java.time.Instant;

public class NetworkDeviceDto {
    private Long id;
    private Long locationId;
    private String locationName;
    private String locationCode;
    private String adminUnitName;
    private String deviceType;
    private String serialNumber;
    private String macAddress;
    private String model;
    private String vendor;
    private String firmwareVersion;
    private String ipAddress;
    private String status;
    private Integer cpuUsagePercent;
    private Integer memoryUsagePercent;
    private Long uptimeSeconds;
    private Instant lastSeenAt;
    private Instant installedAt;

    public NetworkDeviceDto() {}

    public static NetworkDeviceDto from(NetworkDevice dev) {
        NetworkDeviceDto dto = new NetworkDeviceDto();
        dto.setId(dev.getId());
        if (dev.getLocation() != null) {
            dto.setLocationId(dev.getLocation().getId());
            dto.setLocationName(dev.getLocation().getName());
            dto.setLocationCode(dev.getLocation().getLocationCode());
            if (dev.getLocation().getAdminUnit() != null) {
                dto.setAdminUnitName(dev.getLocation().getAdminUnit().getName());
            }
        }
        dto.setDeviceType(dev.getDeviceType());
        dto.setSerialNumber(dev.getSerialNumber());
        dto.setMacAddress(dev.getMacAddress());
        dto.setModel(dev.getModel());
        dto.setVendor(dev.getVendor());
        dto.setFirmwareVersion(dev.getFirmwareVersion());
        dto.setIpAddress(dev.getIpAddress());
        dto.setStatus(dev.getStatus());
        dto.setCpuUsagePercent(dev.getCpuUsagePercent());
        dto.setMemoryUsagePercent(dev.getMemoryUsagePercent());
        dto.setUptimeSeconds(dev.getUptimeSeconds());
        dto.setLastSeenAt(dev.getLastSeenAt());
        dto.setInstalledAt(dev.getInstalledAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public String getAdminUnitName() { return adminUnitName; }
    public void setAdminUnitName(String adminUnitName) { this.adminUnitName = adminUnitName; }

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
}

