package com.wificaptive.gateway.adapter;

import com.wificaptive.gateway.dto.GatewayAuthResult;
import com.wificaptive.gateway.dto.GatewayRevokeResult;
import com.wificaptive.gateway.dto.GatewayStatusDto;
import com.wificaptive.gateway.dto.MockGatewayConfigDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Primary Delegating Gateway Adapter.
 * Dynamically routes gateway authorization requests to the active hardware adapter
 * based on configuration property 'app.gateway.adapter-type':
 *
 * - 'gwn_cloud' / 'grandstream': Grandstream GWN7001 via GWN.Cloud
 * - 'mock': Local simulation engine
 */
@Primary
@Component("gatewayAdapter")
public class DelegatingGatewayAdapter implements GatewayAdapter {

    private static final Logger log = LoggerFactory.getLogger(DelegatingGatewayAdapter.class);

    private final GatewayAdapter mockGatewayAdapter;
    private final GatewayAdapter gwnCloudGatewayAdapter;

    @Value("${app.gateway.adapter-type:gwn_cloud}")
    private String configuredAdapterType;

    public DelegatingGatewayAdapter(
            @Qualifier("mockGatewayAdapter") GatewayAdapter mockGatewayAdapter,
            @Qualifier("gwnCloudGatewayAdapter") GatewayAdapter gwnCloudGatewayAdapter
    ) {
        this.mockGatewayAdapter = mockGatewayAdapter;
        this.gwnCloudGatewayAdapter = gwnCloudGatewayAdapter;
    }

    public GatewayAdapter getActiveDelegate() {
        if (configuredAdapterType == null) {
            return gwnCloudGatewayAdapter;
        }

        String type = configuredAdapterType.trim().toLowerCase();
        return switch (type) {
            case "mock" -> mockGatewayAdapter;
            case "gwn_cloud", "grandstream", "gwn7001" -> gwnCloudGatewayAdapter;
            default -> {
                log.warn("Unknown gateway adapter type '{}', falling back to gwnCloudGatewayAdapter", configuredAdapterType);
                yield gwnCloudGatewayAdapter;
            }
        };
    }

    @Override
    public GatewayAuthResult authorizeSession(Long sessionId, String macAddress, String ipAddress, Integer durationMinutes, Integer maxBandwidthMbps) {
        return getActiveDelegate().authorizeSession(sessionId, macAddress, ipAddress, durationMinutes, maxBandwidthMbps);
    }

    @Override
    public GatewayRevokeResult revokeSession(Long sessionId, String macAddress, String ipAddress, String gatewayReference) {
        return getActiveDelegate().revokeSession(sessionId, macAddress, ipAddress, gatewayReference);
    }

    @Override
    public GatewayStatusDto getStatus() {
        GatewayStatusDto status = getActiveDelegate().getStatus();
        return status;
    }

    @Override
    public GatewayStatusDto updateConfig(MockGatewayConfigDto config) {
        return getActiveDelegate().updateConfig(config);
    }
}

