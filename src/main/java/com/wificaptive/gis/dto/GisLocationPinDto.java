package com.wificaptive.gis.dto;

public class GisLocationPinDto {
    private Long id;
    private String slug;
    private String name;
    private String locationCode;
    private String address;
    private Double latitude;
    private Double longitude;
    private Long adminUnitId;
    private String adminUnitName;
    private String districtName;
    private boolean active;
    private String ispName;
    private String circuitCode;
    private String status; // ONLINE, OFFLINE, DEGRADED
    private int activeSessionsCount;
    private int totalDevices;
    private int onlineDevices;
    private int openIncidentsCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Long getAdminUnitId() { return adminUnitId; }
    public void setAdminUnitId(Long adminUnitId) { this.adminUnitId = adminUnitId; }
    public String getAdminUnitName() { return adminUnitName; }
    public void setAdminUnitName(String adminUnitName) { this.adminUnitName = adminUnitName; }
    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getIspName() { return ispName; }
    public void setIspName(String ispName) { this.ispName = ispName; }
    public String getCircuitCode() { return circuitCode; }
    public void setCircuitCode(String circuitCode) { this.circuitCode = circuitCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getActiveSessionsCount() { return activeSessionsCount; }
    public void setActiveSessionsCount(int activeSessionsCount) { this.activeSessionsCount = activeSessionsCount; }
    public int getTotalDevices() { return totalDevices; }
    public void setTotalDevices(int totalDevices) { this.totalDevices = totalDevices; }
    public int getOnlineDevices() { return onlineDevices; }
    public void setOnlineDevices(int onlineDevices) { this.onlineDevices = onlineDevices; }
    public int getOpenIncidentsCount() { return openIncidentsCount; }
    public void setOpenIncidentsCount(int openIncidentsCount) { this.openIncidentsCount = openIncidentsCount; }
}


