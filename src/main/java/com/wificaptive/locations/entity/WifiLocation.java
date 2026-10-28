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
