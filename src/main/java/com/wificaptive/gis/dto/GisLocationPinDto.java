package com.wificaptive.gis.dto;

public class GisLocationPinDto {
    private Long id;
    private String slug;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String adminUnitName;
    private String ispName;
    private String circuitCode;
    private String status; // ONLINE, OFFLINE, MAINTENANCE
    private int activeSessionsCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getAdminUnitName() { return adminUnitName; }
    public void setAdminUnitName(String adminUnitName) { this.adminUnitName = adminUnitName; }
    public String getIspName() { return ispName; }
    public void setIspName(String ispName) { this.ispName = ispName; }
    public String getCircuitCode() { return circuitCode; }
    public void setCircuitCode(String circuitCode) { this.circuitCode = circuitCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getActiveSessionsCount() { return activeSessionsCount; }
    public void setActiveSessionsCount(int activeSessionsCount) { this.activeSessionsCount = activeSessionsCount; }
}

