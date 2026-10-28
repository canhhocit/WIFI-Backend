package com.wificaptive.locations.dto.excel;

public class LocationImportRowData {
    private int rowNumber;
    private String code;
    private String name;
    private String slug;
    private String adminUnitCode;
    private String address;
    private Double latitude;
    private Double longitude;
    private String ispName;
    private String circuitCode;
    private String contactName;
    private String contactPhone;
    private String routerMac;
    private String apMac;

    public LocationImportRowData() {}

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getAdminUnitCode() { return adminUnitCode; }
    public void setAdminUnitCode(String adminUnitCode) { this.adminUnitCode = adminUnitCode; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getIspName() { return ispName; }
    public void setIspName(String ispName) { this.ispName = ispName; }

    public String getCircuitCode() { return circuitCode; }
    public void setCircuitCode(String circuitCode) { this.circuitCode = circuitCode; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getRouterMac() { return routerMac; }
    public void setRouterMac(String routerMac) { this.routerMac = routerMac; }

    public String getApMac() { return apMac; }
    public void setApMac(String apMac) { this.apMac = apMac; }
}

