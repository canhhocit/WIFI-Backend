package com.wificaptive.network.dto;

import com.wificaptive.network.entity.DnsDomainRule;
import java.time.Instant;

public class DnsDomainRuleDto {
    private Long id;
    private String domainName;
    private String ruleType;
    private String category;
    private String description;
    private boolean active;
    private Instant createdAt;

    public DnsDomainRuleDto() {
    }

    public static DnsDomainRuleDto from(DnsDomainRule entity) {
        DnsDomainRuleDto dto = new DnsDomainRuleDto();
        dto.setId(entity.getId());
        dto.setDomainName(entity.getDomainName());
        dto.setRuleType(entity.getRuleType());
        dto.setCategory(entity.getCategory());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.isActive());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
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

