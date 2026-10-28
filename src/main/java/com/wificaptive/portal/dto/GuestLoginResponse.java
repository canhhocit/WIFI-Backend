package com.wificaptive.portal.dto;

import com.wificaptive.sessions.entity.SessionStatus;
import java.time.Instant;

public class GuestLoginResponse {
    private Long sessionId;
    private String sessionToken;
    private SessionStatus status;
    private boolean simulated;
    private String simulatedNotice;
    private Instant expiresAt;
    private Integer durationMinutes;
    private String locationName;
    private String message;

    public GuestLoginResponse() {
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
