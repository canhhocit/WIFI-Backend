package com.wificaptive.portal.template.dto;

import com.wificaptive.portal.template.entity.PortalTemplate;

public class PortalTemplateDto {
    private Long id;
    private String name;
    private String templateType;
    private String themeColor;
    private String backgroundColor;
    private String logoUrl;
    private String bannerUrl;
    private String title;
    private String welcomeText;
    private String buttonText;
    private String termsText;
    private String layoutConfig;
    private boolean active;

    public static PortalTemplateDto from(PortalTemplate t) {
        PortalTemplateDto dto = new PortalTemplateDto();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setTemplateType(t.getTemplateType());
        dto.setThemeColor(t.getThemeColor());
        dto.setBackgroundColor(t.getBackgroundColor());
        dto.setLogoUrl(t.getLogoUrl());
        dto.setBannerUrl(t.getBannerUrl());
        dto.setTitle(t.getTitle());
        dto.setWelcomeText(t.getWelcomeText());
        dto.setButtonText(t.getButtonText());
        dto.setTermsText(t.getTermsText());
        dto.setLayoutConfig(t.getLayoutConfig());
        dto.setActive(t.isActive());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    public String getThemeColor() { return themeColor; }
    public void setThemeColor(String themeColor) { this.themeColor = themeColor; }
    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getWelcomeText() { return welcomeText; }
    public void setWelcomeText(String welcomeText) { this.welcomeText = welcomeText; }
    public String getButtonText() { return buttonText; }
    public void setButtonText(String buttonText) { this.buttonText = buttonText; }
    public String getTermsText() { return termsText; }
    public void setTermsText(String termsText) { this.termsText = termsText; }
    public String getLayoutConfig() { return layoutConfig; }
    public void setLayoutConfig(String layoutConfig) { this.layoutConfig = layoutConfig; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

