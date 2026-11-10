package com.wificaptive.analytics.dto;

public class DeviceTopologySummaryDto {
    private long totalLocations;
    private long errorLocations;
    private long totalDevices;
    private long errorDevices;

    private long routerOnline;
    private long routerOffline;

    private long switchOnline;
    private long switchOffline;

    private long apOnline;
    private long apOffline;

    private long clientWired;
    private long clientWireless;

    public DeviceTopologySummaryDto() {}

    public DeviceTopologySummaryDto(long totalLocations, long errorLocations, long totalDevices, long errorDevices,
                                    long routerOnline, long routerOffline, long switchOnline, long switchOffline,
                                    long apOnline, long apOffline, long clientWired, long clientWireless) {
        this.totalLocations = totalLocations;
        this.errorLocations = errorLocations;
        this.totalDevices = totalDevices;
        this.errorDevices = errorDevices;
        this.routerOnline = routerOnline;
        this.routerOffline = routerOffline;
        this.switchOnline = switchOnline;
        this.switchOffline = switchOffline;
        this.apOnline = apOnline;
        this.apOffline = apOffline;
        this.clientWired = clientWired;
        this.clientWireless = clientWireless;
    }

    public long getTotalLocations() { return totalLocations; }
    public void setTotalLocations(long totalLocations) { this.totalLocations = totalLocations; }

    public long getErrorLocations() { return errorLocations; }
    public void setErrorLocations(long errorLocations) { this.errorLocations = errorLocations; }

    public long getTotalDevices() { return totalDevices; }
    public void setTotalDevices(long totalDevices) { this.totalDevices = totalDevices; }

    public long getErrorDevices() { return errorDevices; }
    public void setErrorDevices(long errorDevices) { this.errorDevices = errorDevices; }

    public long getRouterOnline() { return routerOnline; }
    public void setRouterOnline(long routerOnline) { this.routerOnline = routerOnline; }

    public long getRouterOffline() { return routerOffline; }
    public void setRouterOffline(long routerOffline) { this.routerOffline = routerOffline; }

    public long getSwitchOnline() { return switchOnline; }
    public void setSwitchOnline(long switchOnline) { this.switchOnline = switchOnline; }

    public long getSwitchOffline() { return switchOffline; }
    public void setSwitchOffline(long switchOffline) { this.switchOffline = switchOffline; }

    public long getApOnline() { return apOnline; }
    public void setApOnline(long apOnline) { this.apOnline = apOnline; }

    public long getApOffline() { return apOffline; }
    public void setApOffline(long apOffline) { this.apOffline = apOffline; }

    public long getClientWired() { return clientWired; }
    public void setClientWired(long clientWired) { this.clientWired = clientWired; }

    public long getClientWireless() { return clientWireless; }
    public void setClientWireless(long clientWireless) { this.clientWireless = clientWireless; }
}

