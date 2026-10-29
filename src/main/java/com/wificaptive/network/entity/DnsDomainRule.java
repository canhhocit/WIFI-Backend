package com.wificaptive.network.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dns_domain_rules")
public class DnsDomainRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "domain_name", nullable = false, unique = true, length = 255)
    private String domainName;

    @Column(name = "rule_type", nullable = false, length = 20)
    private String ruleType; // BLOCKLIST, WHITELIST

    @Column(name = "category", length = 100)
    private String category; // GAMBLING, ADULT, MALWARE, GOV_PORTAL, PUBLIC_SERVICE

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public DnsDomainRule() {
    }

    public DnsDomainRule(String domainName, String ruleType, String category, String description, boolean active) {
        this.domainName = domainName;
        this.ruleType = ruleType;
        this.category = category;
        this.description = description;
        this.active = active;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

