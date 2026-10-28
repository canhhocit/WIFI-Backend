package com.wificaptive.devices.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mac_address", nullable = false, unique = true, length = 17)
    private String macAddress;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "hostname", length = 100)
    private String hostname;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "os_type", length = 50)
    private String osType;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "is_blocked", nullable = false)
    private boolean blocked = false;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.firstSeenAt == null) this.firstSeenAt = now;
        if (this.lastSeenAt == null) this.lastSeenAt = now;
    }

    public Device() {
    }

    public Device(String macAddress, String ipAddress, String hostname, String userAgent, String osType) {
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.hostname = hostname;
        this.userAgent = userAgent;
        this.osType = osType;
        Instant now = Instant.now();
        this.firstSeenAt = now;
        this.lastSeenAt = now;
        this.blocked = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
