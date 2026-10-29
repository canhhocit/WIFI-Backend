package com.wificaptive.network.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "network_bandwidth_policies")
public class NetworkBandwidthPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "download_speed_kbps", nullable = false)
    private Integer downloadSpeedKbps = 5120; // 5 Mbps

    @Column(name = "upload_speed_kbps", nullable = false)
    private Integer uploadSpeedKbps = 2048; // 2 Mbps

    @Column(name = "max_session_seconds", nullable = false)
    private Integer maxSessionSeconds = 5400; // 90 mins

    @Column(name = "quota_mb")
    private Integer quotaMb = 0; // 0 = unlimited

    @Column(nullable = false)
    private Integer priority = 1;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public NetworkBandwidthPolicy() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDownloadSpeedKbps() { return downloadSpeedKbps; }
    public void setDownloadSpeedKbps(Integer downloadSpeedKbps) { this.downloadSpeedKbps = downloadSpeedKbps; }

    public Integer getUploadSpeedKbps() { return uploadSpeedKbps; }
    public void setUploadSpeedKbps(Integer uploadSpeedKbps) { this.uploadSpeedKbps = uploadSpeedKbps; }

    public Integer getMaxSessionSeconds() { return maxSessionSeconds; }
    public void setMaxSessionSeconds(Integer maxSessionSeconds) { this.maxSessionSeconds = maxSessionSeconds; }

    public Integer getQuotaMb() { return quotaMb; }
    public void setQuotaMb(Integer quotaMb) { this.quotaMb = quotaMb; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
