package com.wificaptive.devices.dto;

public class AssignDeviceLocationRequest {

    private Long locationId;
    private String name;
    private String gwnNetworkId;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGwnNetworkId() { return gwnNetworkId; }
    public void setGwnNetworkId(String gwnNetworkId) { this.gwnNetworkId = gwnNetworkId; }
}
