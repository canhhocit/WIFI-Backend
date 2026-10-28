package com.wificaptive.devices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class DeviceSwapRequest {

    @NotNull(message = "ID điểm phát sóng không được để trống")
    private Long locationId;

    private Long oldDeviceId;

    @NotBlank(message = "Loại thiết bị không được để trống (ONT, ROUTER, ACCESS_POINT)")
    private String deviceType;

    @NotBlank(message = "Số Serial thiết bị mới không được để trống")
    private String newSerialNumber;

    @NotBlank(message = "Địa chỉ MAC thiết bị mới không được để trống")
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", message = "Địa chỉ MAC phải đúng chuẩn (XX:XX:XX:XX:XX:XX)")
    private String newMacAddress;

    private String newModel;
    private String newVendor;

    @NotBlank(message = "Lý do thay thế không được để trống")
    private String swapReason;

    public DeviceSwapRequest() {}

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public Long getOldDeviceId() { return oldDeviceId; }
    public void setOldDeviceId(Long oldDeviceId) { this.oldDeviceId = oldDeviceId; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getNewSerialNumber() { return newSerialNumber; }
    public void setNewSerialNumber(String newSerialNumber) { this.newSerialNumber = newSerialNumber; }

    public String getNewMacAddress() { return newMacAddress; }
    public void setNewMacAddress(String newMacAddress) { this.newMacAddress = newMacAddress; }

    public String getNewModel() { return newModel; }
    public void setNewModel(String newModel) { this.newModel = newModel; }

    public String getNewVendor() { return newVendor; }
    public void setNewVendor(String newVendor) { this.newVendor = newVendor; }

    public String getSwapReason() { return swapReason; }
    public void setSwapReason(String swapReason) { this.swapReason = swapReason; }
}

