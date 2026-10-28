package com.wificaptive.portal.dto;

import com.wificaptive.locations.dto.LocationDto;

public class PortalInitResponse {
    private LocationDto location;
    private String clientIp;
    private String clientMacHint;
    private boolean mockGateway;
    private String gatewayNotice;

    public PortalInitResponse() {
    }

    public PortalInitResponse(LocationDto location, String clientIp, String clientMacHint, boolean mockGateway, String gatewayNotice) {
        this.location = location;
        this.clientIp = clientIp;
        this.clientMacHint = clientMacHint;
        this.mockGateway = mockGateway;
        this.gatewayNotice = gatewayNotice;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getClientMacHint() {
        return clientMacHint;
    }

    public void setClientMacHint(String clientMacHint) {
        this.clientMacHint = clientMacHint;
    }

    public boolean isMockGateway() {
        return mockGateway;
    }

    public void setMockGateway(boolean mockGateway) {
        this.mockGateway = mockGateway;
    }

    public String getGatewayNotice() {
        return gatewayNotice;
    }

    public void setGatewayNotice(String gatewayNotice) {
        this.gatewayNotice = gatewayNotice;
    }
}
