package com.wificaptive.gateway;

import com.wificaptive.gateway.adapter.MockGatewayAdapter;
import com.wificaptive.gateway.dto.GatewayAuthResult;
import com.wificaptive.gateway.dto.GatewayRevokeResult;
import com.wificaptive.gateway.dto.GatewayStatusDto;
import com.wificaptive.gateway.dto.MockGatewayConfigDto;
import com.wificaptive.gateway.repository.GatewayTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MockGatewayAdapterTest {

    @Mock
    private GatewayTransactionRepository transactionRepository;

    private MockGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockGatewayAdapter(transactionRepository, false, 0);
    }

    @Test
    void testAuthorizeSessionSuccess() {
        GatewayAuthResult result = adapter.authorizeSession(101L, "00:11:22:33:44:55", "192.168.1.50", 120, 20);

        assertTrue(result.isSuccess());
        assertTrue(result.isSimulated());
        assertNotNull(result.getGatewayReference());
        assertTrue(result.getGatewayReference().startsWith("MOCK-GW-AUTH-"));
    }

    @Test
    void testAuthorizeSessionSimulatedFailure() {
        adapter.updateConfig(new MockGatewayConfigDto(true, 0));

        GatewayAuthResult result = adapter.authorizeSession(102L, "00:11:22:33:44:55", "192.168.1.50", 120, 20);

        assertFalse(result.isSuccess());
        assertTrue(result.isSimulated());
        assertNull(result.getGatewayReference());
        assertEquals(502, result.getStatusCode());
        assertTrue(result.getMessage().contains("Simulated Gateway Failure"));
    }

    @Test
    void testRevokeSessionSuccess() {
        GatewayRevokeResult result = adapter.revokeSession(101L, "00:11:22:33:44:55", "192.168.1.50", "MOCK-REF-123");

        assertTrue(result.isSuccess());
        assertTrue(result.isSimulated());
    }

    @Test
    void testGetStatusReflectsFailureMode() {
        GatewayStatusDto statusNormal = adapter.getStatus();
        assertTrue(statusNormal.isHealthy());
        assertFalse(statusNormal.isSimulateFailures());

        adapter.updateConfig(new MockGatewayConfigDto(true, 50));
        GatewayStatusDto statusFail = adapter.getStatus();
        assertFalse(statusFail.isHealthy());
        assertTrue(statusFail.isSimulateFailures());
        assertEquals(50, statusFail.getSimulatedLatencyMs());
    }
}
