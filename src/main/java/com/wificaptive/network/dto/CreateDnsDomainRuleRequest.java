package com.wificaptive.network.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateDnsDomainRuleRequest {

    @NotBlank(message = "Tên miền không được để trống")
    private String domainName;

    @NotBlank(message = "Loại quy tắc không được để trống")
    @Pattern(regexp = "BLOCKLIST|WHITELIST", message = "Loại quy tắc phải là BLOCKLIST hoặc WHITELIST")
    private String ruleType;

    private String category;
    private String description;
    private Boolean active = true;

    public CreateDnsDomainRuleRequest() {
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}

