package com.wificaptive.verification.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "remembered_devices")
public class RememberedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_mac", nullable = false, unique = true, length = 50)
    private String clientMac;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "remember_duration_days", nullable = false)
    private int rememberDurationDays = 30;

    @Column(name = "bypass_otp_until", nullable = false)
    private Instant bypassOtpUntil;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RememberedDevice() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClientMac() { return clientMac; }
    public void setClientMac(String clientMac) { this.clientMac = clientMac; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public int getRememberDurationDays() { return rememberDurationDays; }
    public void setRememberDurationDays(int rememberDurationDays) { this.rememberDurationDays = rememberDurationDays; }

    public Instant getBypassOtpUntil() { return bypassOtpUntil; }
    public void setBypassOtpUntil(Instant bypassOtpUntil) { this.bypassOtpUntil = bypassOtpUntil; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

