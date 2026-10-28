package com.wificaptive.locations.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateLocationRequest {

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;

    @NotBlank(message = "Location name is required")
    private String name;

    private String address;

    private String description;

    @Min(value = 1, message = "Session duration must be at least 1 minute")
    @Max(value = 1440, message = "Session duration cannot exceed 24 hours (1440 minutes)")
    private Integer sessionDurationMinutes = 120;

    @Min(value = 1, message = "Max bandwidth must be at least 1 Mbps")
    @Max(value = 1000, message = "Max bandwidth cannot exceed 1000 Mbps")
    private Integer maxBandwidthMbps = 20;

    @NotBlank(message = "Terms and conditions are required")
    private String termsAndConditions;

    private String welcomeMessage = "Welcome to High-Speed Wi-Fi";

    private String promoTitle;
    private String promoContent;
    private String promoBannerUrl;
    private String promoActionUrl;

    private boolean active = true;

    public CreateLocationRequest() {
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
