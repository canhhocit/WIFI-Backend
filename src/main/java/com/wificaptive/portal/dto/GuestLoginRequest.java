package com.wificaptive.portal.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class GuestLoginRequest {

    @NotBlank(message = "Location slug is required")
    private String locationSlug;

    private String macAddress;

    private String clientIp;

    private String guestName;

    private String guestContact;

    @AssertTrue(message = "You must accept the Terms and Conditions to proceed")
    private boolean consentGiven;

    public GuestLoginRequest() {
    }

    public String getLocationSlug() {
        return locationSlug;
    }

    public void setLocationSlug(String locationSlug) {
        this.locationSlug = locationSlug;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestContact() {
        return guestContact;
    }

    public void setGuestContact(String guestContact) {
        this.guestContact = guestContact;
    }

    public boolean isConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(boolean consentGiven) {
        this.consentGiven = consentGiven;
    }
}
