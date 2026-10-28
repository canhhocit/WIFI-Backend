package com.wificaptive.sessions.entity;

import com.wificaptive.common.entity.BaseEntity;
import com.wificaptive.devices.entity.Device;
import com.wificaptive.locations.entity.WifiLocation;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "guest_sessions")
public class GuestSession extends BaseEntity {

    @Column(name = "session_token", nullable = false, unique = true, length = 64)
    private String sessionToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WifiLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "mac_address", nullable = false, length = 17)
    private String macAddress;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Column(name = "guest_contact", length = 100)
    private String guestContact;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven = true;

    @Column(name = "consent_timestamp", nullable = false)
    private Instant consentTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status = SessionStatus.PENDING;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 50)
    private String revokedBy;

    @Column(name = "revoke_reason", length = 255)
    private String revokeReason;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Column(name = "bytes_downloaded")
    private Long bytesDownloaded = 0L;

    @Column(name = "bytes_uploaded")
    private Long bytesUploaded = 0L;

    public GuestSession() {
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public WifiLocation getLocation() {
        return location;
    }

    public void setLocation(WifiLocation location) {
        this.location = location;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
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
}
