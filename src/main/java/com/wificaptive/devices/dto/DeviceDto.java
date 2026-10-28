package com.wificaptive.devices.dto;

import com.wificaptive.devices.entity.Device;
import java.time.Instant;

public class DeviceDto {
    private Long id;
    private String macAddress;
    private String ipAddress;
    private String hostname;
    private String userAgent;
    private String osType;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
    private boolean blocked;

    public DeviceDto() {
    }

    public static DeviceDto from(Device device) {
        DeviceDto dto = new DeviceDto();
        dto.setId(device.getId());
        dto.setMacAddress(device.getMacAddress());
        dto.setIpAddress(device.getIpAddress());
        dto.setHostname(device.getHostname());
        dto.setUserAgent(device.getUserAgent());
        dto.setOsType(device.getOsType());
        dto.setFirstSeenAt(device.getFirstSeenAt());
        dto.setLastSeenAt(device.getLastSeenAt());
        dto.setBlocked(device.isBlocked());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
