package com.wificaptive.portal.dto;

import com.wificaptive.sessions.entity.SessionStatus;
import java.time.Instant;

public class GuestSessionStatusResponse {
    private String sessionToken;
    private SessionStatus status;
    private boolean simulated;
    private String simulatedNotice;
    private Long remainingSeconds;
    private Instant expiresAt;
    private Instant authorizedAt;
    private String locationName;
    private String macAddress;
    private String ipAddress;
    private Integer maxBandwidthMbps;
    private String revokeReason;

    public GuestSessionStatusResponse() {
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public String getSimulatedNotice() {
        return simulatedNotice;
    }

    public void setSimulatedNotice(String simulatedNotice) {
        this.simulatedNotice = simulatedNotice;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(Instant authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
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

    public Integer getMaxBandwidthMbps() {
        return maxBandwidthMbps;
    }

    public void setMaxBandwidthMbps(Integer maxBandwidthMbps) {
        this.maxBandwidthMbps = maxBandwidthMbps;
    }

    public String getRevokeReason() {
        return revokeReason;
    }

    public void setRevokeReason(String revokeReason) {
        this.revokeReason = revokeReason;
    }
}
