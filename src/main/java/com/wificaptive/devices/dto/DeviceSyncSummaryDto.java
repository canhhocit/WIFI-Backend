package com.wificaptive.devices.dto;

import java.util.List;

public class DeviceSyncSummaryDto {
    private int syncedCount;
    private int updatedCount;
    private int createdCount;
    private List<NetworkDeviceDto> devices;
    private String message;
    private boolean simulated;

    public DeviceSyncSummaryDto() {}

    public DeviceSyncSummaryDto(int syncedCount, int updatedCount, int createdCount,
                                List<NetworkDeviceDto> devices, String message, boolean simulated) {
        this.syncedCount = syncedCount;
        this.updatedCount = updatedCount;
        this.createdCount = createdCount;
        this.devices = devices;
        this.message = message;
        this.simulated = simulated;
    }

    public int getSyncedCount() { return syncedCount; }
    public void setSyncedCount(int syncedCount) { this.syncedCount = syncedCount; }

    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }

    public int getCreatedCount() { return createdCount; }
    public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }

    public List<NetworkDeviceDto> getDevices() { return devices; }
    public void setDevices(List<NetworkDeviceDto> devices) { this.devices = devices; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }
}

