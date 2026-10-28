package com.wificaptive.portal.dto;

import com.wificaptive.campaigns.dto.CampaignDto;
import com.wificaptive.locations.dto.LocationDto;
import com.wificaptive.portal.template.dto.PortalTemplateDto;
import com.wificaptive.surveys.dto.SurveyDto;

public class PortalInitResponse {
    private LocationDto location;
    private String clientIp;
    private String clientMacHint;
    private boolean mockGateway;
    private String gatewayNotice;
    private CampaignDto activeCampaign;
    private SurveyDto activeSurvey;
    private PortalTemplateDto portalTemplate;
    private boolean macRemembered;
    private boolean otpRequired;

    public PortalInitResponse() {
    }

    public PortalInitResponse(LocationDto location, String clientIp, String clientMacHint, boolean mockGateway, String gatewayNotice) {
        this.location = location;
        this.clientIp = clientIp;
        this.clientMacHint = clientMacHint;
        this.mockGateway = mockGateway;
        this.gatewayNotice = gatewayNotice;
    }

    public LocationDto getLocation() { return location; }
    public void setLocation(LocationDto location) { this.location = location; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getClientMacHint() { return clientMacHint; }
    public void setClientMacHint(String clientMacHint) { this.clientMacHint = clientMacHint; }

    public boolean isMockGateway() { return mockGateway; }
    public void setMockGateway(boolean mockGateway) { this.mockGateway = mockGateway; }

    public String getGatewayNotice() { return gatewayNotice; }
    public void setGatewayNotice(String gatewayNotice) { this.gatewayNotice = gatewayNotice; }

    public CampaignDto getActiveCampaign() { return activeCampaign; }
    public void setActiveCampaign(CampaignDto activeCampaign) { this.activeCampaign = activeCampaign; }

    public SurveyDto getActiveSurvey() { return activeSurvey; }
    public void setActiveSurvey(SurveyDto activeSurvey) { this.activeSurvey = activeSurvey; }

    public PortalTemplateDto getPortalTemplate() { return portalTemplate; }
    public void setPortalTemplate(PortalTemplateDto portalTemplate) { this.portalTemplate = portalTemplate; }

    public boolean isMacRemembered() { return macRemembered; }
    public void setMacRemembered(boolean macRemembered) { this.macRemembered = macRemembered; }

    public boolean isOtpRequired() { return otpRequired; }
    public void setOtpRequired(boolean otpRequired) { this.otpRequired = otpRequired; }
}
