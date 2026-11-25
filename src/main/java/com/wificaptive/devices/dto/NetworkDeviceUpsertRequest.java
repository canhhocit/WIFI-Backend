package com.wificaptive.devices.dto;

import jakarta.validation.constraints.NotBlank;

public class NetworkDeviceUpsertRequest {

    private Long locationId;

    private String name;

    @NotBlank(message = "Loại thiết bị không được để trống (ROUTER, ACCESS_POINT, ONT, SWITCH)")
    private String deviceType;

    @NotBlank(message = "Địa chỉ MAC không được để trống")
    private String macAddress;

    @NotBlank(message = "Số Serial (S/N) không được để trống")
    private String serialNumber;

    private String model;

    private String vendor;

    private String ipAddress;

    private String gwnNetworkId;

    private String status;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getGwnNetworkId() { return gwnNetworkId; }
    public void setGwnNetworkId(String gwnNetworkId) { this.gwnNetworkId = gwnNetworkId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
