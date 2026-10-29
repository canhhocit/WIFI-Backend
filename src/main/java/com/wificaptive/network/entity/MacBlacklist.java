package com.wificaptive.network.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mac_blacklist")
public class MacBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mac_address", nullable = false, unique = true, length = 17)
    private String macAddress;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "blocked_by", nullable = false, length = 100)
    private String blockedBy;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt = Instant.now();

    @Column(nullable = false)
    private Boolean active = true;

    public MacBlacklist() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getBlockedBy() { return blockedBy; }
    public void setBlockedBy(String blockedBy) { this.blockedBy = blockedBy; }

    public Instant getBlockedAt() { return blockedAt; }
    public void setBlockedAt(Instant blockedAt) { this.blockedAt = blockedAt; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
