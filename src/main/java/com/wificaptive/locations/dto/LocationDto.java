package com.wificaptive.locations.dto;

import com.wificaptive.locations.entity.WifiLocation;
import java.time.Instant;

public class LocationDto {
    private Long id;
    private String slug;
    private String name;
    private String address;
    private String description;
    private Integer sessionDurationMinutes;
    private Integer maxBandwidthMbps;
    private String termsAndConditions;
    private String welcomeMessage;
    private String promoTitle;
    private String promoContent;
    private String promoBannerUrl;
    private String promoActionUrl;
    private Long adminUnitId;
    private String adminUnitName;
    private String locationCode;
    private Double latitude;
    private Double longitude;
    private String status;
    private String statusReason;
    private String ispName;
    private String circuitCode;
    private String contactName;
    private String contactPhone;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public LocationDto() {
    }

    public static LocationDto from(WifiLocation loc) {
        LocationDto dto = new LocationDto();
        dto.setId(loc.getId());
        dto.setSlug(loc.getSlug());
        dto.setName(loc.getName());
        dto.setAddress(loc.getAddress());
        dto.setDescription(loc.getDescription());
        dto.setSessionDurationMinutes(loc.getSessionDurationMinutes());
        dto.setMaxBandwidthMbps(loc.getMaxBandwidthMbps());
        dto.setTermsAndConditions(loc.getTermsAndConditions());
        dto.setWelcomeMessage(loc.getWelcomeMessage());
        dto.setPromoTitle(loc.getPromoTitle());
        dto.setPromoContent(loc.getPromoContent());
        dto.setPromoBannerUrl(loc.getPromoBannerUrl());
        dto.setPromoActionUrl(loc.getPromoActionUrl());
        if (loc.getAdminUnit() != null) {
            dto.setAdminUnitId(loc.getAdminUnit().getId());
            dto.setAdminUnitName(loc.getAdminUnit().getName());
        }
        dto.setLocationCode(loc.getLocationCode());
        dto.setLatitude(loc.getLatitude());
        dto.setLongitude(loc.getLongitude());
        dto.setStatus(loc.getStatus() != null ? loc.getStatus() : (loc.isActive() ? "ACTIVE" : "INACTIVE"));
        dto.setStatusReason(loc.getStatusReason());
        dto.setIspName(loc.getIspName());
        dto.setCircuitCode(loc.getCircuitCode());
        dto.setContactName(loc.getContactName());
        dto.setContactPhone(loc.getContactPhone());
        dto.setActive(loc.isActive());
        dto.setCreatedAt(loc.getCreatedAt());
        dto.setUpdatedAt(loc.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSessionDurationMinutes() {
        return sessionDurationMinutes;
    }

    public void setSessionDurationMinutes(Integer sessionDurationMinutes) {
        this.sessionDurationMinutes = sessionDurationMinutes;
    }

    public Integer getMaxBandwidthMbps() {
        return maxBandwidthMbps;
    }

    public void setMaxBandwidthMbps(Integer maxBandwidthMbps) {
        this.maxBandwidthMbps = maxBandwidthMbps;
    }

    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPromoTitle() {
        return promoTitle;
    }

    public void setPromoTitle(String promoTitle) {
        this.promoTitle = promoTitle;
    }

    public String getPromoContent() {
        return promoContent;
    }

    public void setPromoContent(String promoContent) {
        this.promoContent = promoContent;
    }

    public String getPromoBannerUrl() {
        return promoBannerUrl;
    }

    public void setPromoBannerUrl(String promoBannerUrl) {
        this.promoBannerUrl = promoBannerUrl;
    }

    public String getPromoActionUrl() {
        return promoActionUrl;
    }

    public void setPromoActionUrl(String promoActionUrl) {
        this.promoActionUrl = promoActionUrl;
    }

    public Long getAdminUnitId() { return adminUnitId; }
    public void setAdminUnitId(Long adminUnitId) { this.adminUnitId = adminUnitId; }

    public String getAdminUnitName() { return adminUnitName; }
    public void setAdminUnitName(String adminUnitName) { this.adminUnitName = adminUnitName; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }

    public String getIspName() { return ispName; }
    public void setIspName(String ispName) { this.ispName = ispName; }

    public String getCircuitCode() { return circuitCode; }
    public void setCircuitCode(String circuitCode) { this.circuitCode = circuitCode; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
}
