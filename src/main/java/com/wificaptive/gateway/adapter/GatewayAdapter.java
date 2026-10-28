package com.wificaptive.gateway.adapter;

import com.wificaptive.gateway.dto.GatewayAuthResult;
import com.wificaptive.gateway.dto.GatewayRevokeResult;
import com.wificaptive.gateway.dto.GatewayStatusDto;
import com.wificaptive.gateway.dto.MockGatewayConfigDto;

public interface GatewayAdapter {

    /**
     * Authorize network access for the given client device session.
     */
    GatewayAuthResult authorizeSession(Long sessionId, String macAddress, String ipAddress, Integer durationMinutes, Integer maxBandwidthMbps);

    /**
     * Revoke network access for the given client device session.
     */
    GatewayRevokeResult revokeSession(Long sessionId, String macAddress, String ipAddress, String gatewayReference);

    /**
     * Retrieve current operational status and health metrics of the gateway.
     */
    GatewayStatusDto getStatus();

    /**
     * Configure runtime simulation parameters (only effective for mock adapter).
     */
    GatewayStatusDto updateConfig(MockGatewayConfigDto config);
}
