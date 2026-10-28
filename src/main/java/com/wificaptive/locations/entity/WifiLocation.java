package com.wificaptive.locations.entity;

import com.wificaptive.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "wifi_locations")
public class WifiLocation extends BaseEntity {

    @Column(name = "slug", nullable = false, unique = true, length = 60)
    private String slug;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "session_duration_minutes", nullable = false)
    private Integer sessionDurationMinutes = 120;

    @Column(name = "max_bandwidth_mbps", nullable = false)
    private Integer maxBandwidthMbps = 20;

    @Column(name = "terms_and_conditions", nullable = false, columnDefinition = "TEXT")
    private String termsAndConditions;

    @Column(name = "welcome_message", length = 255)
    private String welcomeMessage = "Welcome to High-Speed Wi-Fi";

    @Column(name = "promo_title", length = 150)
    private String promoTitle;

    @Column(name = "promo_content", columnDefinition = "TEXT")
    private String promoContent;

    @Column(name = "promo_banner_url", length = 500)
    private String promoBannerUrl;

    @Column(name = "promo_action_url", length = 500)
    private String promoActionUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_unit_id")
    private com.wificaptive.geography.entity.AdministrativeUnit adminUnit;

    @Column(name = "location_code", length = 50)
    private String locationCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    @Column(name = "status_reason", length = 255)
    private String statusReason;

    @Column(name = "isp_name", length = 50)
    private String ispName;

    @Column(name = "circuit_code", length = 100)
    private String circuitCode;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public WifiLocation() {
    }

    public WifiLocation(String slug, String name, String address, String description,
                        Integer sessionDurationMinutes, Integer maxBandwidthMbps,
                        String termsAndConditions, String welcomeMessage, boolean active) {
        this.slug = slug;
        this.name = name;
        this.address = address;
        this.description = description;
        this.sessionDurationMinutes = sessionDurationMinutes;
        this.maxBandwidthMbps = maxBandwidthMbps;
        this.termsAndConditions = termsAndConditions;
        this.welcomeMessage = welcomeMessage;
        this.active = active;
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

    public com.wificaptive.geography.entity.AdministrativeUnit getAdminUnit() { return adminUnit; }
    public void setAdminUnit(com.wificaptive.geography.entity.AdministrativeUnit adminUnit) { this.adminUnit = adminUnit; }
    public Long getAdminUnitId() { return adminUnit != null ? adminUnit.getId() : null; }

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
}
