package com.wificaptive.sessions.dto;

import com.wificaptive.sessions.entity.GuestSession;
import com.wificaptive.sessions.entity.SessionStatus;

import java.time.Instant;

public class GuestSessionDto {
    private Long id;
    private String sessionToken;
    private Long locationId;
    private String locationName;
    private String locationSlug;
    private Long deviceId;
    private String macAddress;
    private String ipAddress;
    private String guestName;
    private String guestContact;
    private boolean consentGiven;
    private Instant consentTimestamp;
    private SessionStatus status;
    private Instant authorizedAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private String revokedBy;
    private String revokeReason;
    private String failureReason;
    private String gatewayReference;
    private Long bytesDownloaded;
    private Long bytesUploaded;
    private Long remainingSeconds;
    private Instant createdAt;

    public GuestSessionDto() {
    }

    public static GuestSessionDto from(GuestSession session) {
        GuestSessionDto dto = new GuestSessionDto();
        dto.setId(session.getId());
        dto.setSessionToken(session.getSessionToken());
        if (session.getLocation() != null) {
            dto.setLocationId(session.getLocation().getId());
            dto.setLocationName(session.getLocation().getName());
            dto.setLocationSlug(session.getLocation().getSlug());
        }
        if (session.getDevice() != null) {
            dto.setDeviceId(session.getDevice().getId());
        }
        dto.setMacAddress(session.getMacAddress());
        dto.setIpAddress(session.getIpAddress());
        dto.setGuestName(session.getGuestName());
        dto.setGuestContact(session.getGuestContact());
        dto.setConsentGiven(session.isConsentGiven());
        dto.setConsentTimestamp(session.getConsentTimestamp());
        dto.setStatus(session.getStatus());
        dto.setAuthorizedAt(session.getAuthorizedAt());
        dto.setExpiresAt(session.getExpiresAt());
        dto.setRevokedAt(session.getRevokedAt());
        dto.setRevokedBy(session.getRevokedBy());
        dto.setRevokeReason(session.getRevokeReason());
        dto.setFailureReason(session.getFailureReason());
        dto.setGatewayReference(session.getGatewayReference());
        dto.setBytesDownloaded(session.getBytesDownloaded());
        dto.setBytesUploaded(session.getBytesUploaded());
        dto.setCreatedAt(session.getCreatedAt());

        if (session.getStatus() == SessionStatus.AUTHORIZED && session.getExpiresAt() != null) {
            long remaining = (session.getExpiresAt().toEpochMilli() - Instant.now().toEpochMilli()) / 1000;
            dto.setRemainingSeconds(Math.max(0, remaining));
        } else {
            dto.setRemainingSeconds(0L);
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationSlug() {
        return locationSlug;
    }

    public void setLocationSlug(String locationSlug) {
        this.locationSlug = locationSlug;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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

    public Instant getConsentTimestamp() {
        return consentTimestamp;
    }

    public void setConsentTimestamp(Instant consentTimestamp) {
        this.consentTimestamp = consentTimestamp;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(Instant authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }

    public String getRevokeReason() {
        return revokeReason;
    }

    public void setRevokeReason(String revokeReason) {
        this.revokeReason = revokeReason;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    public Long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public void setBytesDownloaded(Long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

    public Long getBytesUploaded() {
        return bytesUploaded;
    }

    public void setBytesUploaded(Long bytesUploaded) {
        this.bytesUploaded = bytesUploaded;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
