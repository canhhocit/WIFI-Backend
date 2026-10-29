package com.wificaptive.network.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "access_logs_retention")
public class AccessLogRetention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "session_token", length = 100)
    private String sessionToken;

    @Column(name = "client_mac", nullable = false, length = 17)
    private String clientMac;

    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "location_name", length = 150)
    private String locationName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "authenticated_at", nullable = false)
    private Instant authenticatedAt;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds = 0L;

    @Column(name = "bytes_in")
    private Long bytesIn = 0L;

    @Column(name = "bytes_out")
    private Long bytesOut = 0L;

    @Column(name = "termination_reason", length = 50)
    private String terminationReason = "USER_LOGOUT";

    @Column(name = "retained_until", nullable = false)
    private Instant retainedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AccessLogRetention() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public String getClientMac() { return clientMac; }
    public void setClientMac(String clientMac) { this.clientMac = clientMac; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Instant getAuthenticatedAt() { return authenticatedAt; }
    public void setAuthenticatedAt(Instant authenticatedAt) { this.authenticatedAt = authenticatedAt; }

    public Instant getDisconnectedAt() { return disconnectedAt; }
    public void setDisconnectedAt(Instant disconnectedAt) { this.disconnectedAt = disconnectedAt; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public Long getBytesIn() { return bytesIn; }
    public void setBytesIn(Long bytesIn) { this.bytesIn = bytesIn; }

    public Long getBytesOut() { return bytesOut; }
    public void setBytesOut(Long bytesOut) { this.bytesOut = bytesOut; }

    public String getTerminationReason() { return terminationReason; }
    public void setTerminationReason(String terminationReason) { this.terminationReason = terminationReason; }

    public Instant getRetainedUntil() { return retainedUntil; }
    public void setRetainedUntil(Instant retainedUntil) { this.retainedUntil = retainedUntil; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
