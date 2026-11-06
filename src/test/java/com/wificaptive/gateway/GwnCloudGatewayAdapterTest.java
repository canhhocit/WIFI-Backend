package com.wificaptive.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter;
import com.wificaptive.gateway.dto.GatewayAuthResult;
import com.wificaptive.gateway.dto.GatewayRevokeResult;
import com.wificaptive.gateway.dto.GatewayStatusDto;
import com.wificaptive.gateway.repository.GatewayTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GwnCloudGatewayAdapterTest {

    @Mock
    private GatewayTransactionRepository transactionRepository;

    private GwnCloudGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GwnCloudGatewayAdapter(transactionRepository, new ObjectMapper());
    }

    @Test
    void testStandbyModeAuthorizationWhenNoCredentials() {
        ReflectionTestUtils.setField(adapter, "appId", "");
        ReflectionTestUtils.setField(adapter, "appSecret", "");

        GatewayAuthResult result = adapter.authorizeSession(101L, "24:0B:2B:B4:48:0C", "192.168.80.50", 120, 20);

        assertTrue(result.isSuccess());
        assertTrue(result.isSimulated());
        assertNotNull(result.getGatewayReference());
        assertTrue(result.getGatewayReference().startsWith("GWN7002-STBY-"));
    }

    @Test
    void testStandbyModeRevocationWhenNoCredentials() {
        ReflectionTestUtils.setField(adapter, "appId", "");
        ReflectionTestUtils.setField(adapter, "appSecret", "");

        GatewayRevokeResult result = adapter.revokeSession(101L, "24:0B:2B:B4:48:0C", "192.168.80.50", "GWN7002-STBY-12345678");

        assertTrue(result.isSuccess());
        assertTrue(result.isSimulated());
    }

    @Test
    void testGetStatusReflectsStandbyAndConfiguredModes() {
        ReflectionTestUtils.setField(adapter, "appId", "");
        ReflectionTestUtils.setField(adapter, "appSecret", "");

        GatewayStatusDto statusStandby = adapter.getStatus();
        assertTrue(statusStandby.isHealthy());
        assertTrue(statusStandby.isSimulated());
        assertEquals("GWN_CLOUD", statusStandby.getAdapterType());

        ReflectionTestUtils.setField(adapter, "appId", "667870");
        ReflectionTestUtils.setField(adapter, "appSecret", "secret123");
        ReflectionTestUtils.setField(adapter, "gwnBaseUrl", "https://oc.gwn.cloud");

        GatewayStatusDto statusConfigured = adapter.getStatus();
        assertTrue(statusConfigured.isHealthy());
        assertFalse(statusConfigured.isSimulated());
        assertTrue(statusConfigured.getStatusMessage().contains("https://oc.gwn.cloud"));
    }
}
