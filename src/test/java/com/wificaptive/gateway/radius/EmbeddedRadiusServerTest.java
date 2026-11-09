package com.wificaptive.gateway.radius;

import com.wificaptive.sessions.entity.GuestSession;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusClient;

import java.net.InetAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddedRadiusServerTest {

    @Mock
    private GuestSessionRepository sessionRepository;

    private EmbeddedRadiusServer radiusServer;
    private static final int TEST_AUTH_PORT = 11812;
    private static final int TEST_ACCT_PORT = 11813;
    private static final String SHARED_SECRET = "TestSecret@2026";

    @BeforeEach
    void setUp() {
        radiusServer = new EmbeddedRadiusServer(sessionRepository);
        ReflectionTestUtils.setField(radiusServer, "radiusEnabled", true);
        ReflectionTestUtils.setField(radiusServer, "radiusAuthPort", TEST_AUTH_PORT);
        ReflectionTestUtils.setField(radiusServer, "radiusAcctPort", TEST_ACCT_PORT);
        ReflectionTestUtils.setField(radiusServer, "sharedSecret", SHARED_SECRET);
        radiusServer.start();
    }

    @AfterEach
    void tearDown() {
        if (radiusServer != null) {
            radiusServer.stop();
        }
    }

    @Test
    void testAccessRequest_AuthorizedSession_ReturnsAccessAccept() throws Exception {
        // Prepare mock session
        String testMac = "A8:64:F1:E2:C3:95";
        GuestSession session = new GuestSession();
        ReflectionTestUtils.setField(session, "id", 101L);
        ReflectionTestUtils.setField(session, "macAddress", testMac);
        ReflectionTestUtils.setField(session, "status", SessionStatus.AUTHORIZED);
        ReflectionTestUtils.setField(session, "sessionToken", "sess_test12345678");
        ReflectionTestUtils.setField(session, "expiresAt", Instant.now().plus(2, ChronoUnit.HOURS));

        when(sessionRepository.findAll()).thenReturn(List.of(session));

        // Use TinyRadius client to send test UDP Access-Request
        RadiusClient client = new RadiusClient("127.0.0.1", SHARED_SECRET);
        client.setAuthPort(TEST_AUTH_PORT);
        client.setSocketTimeout(3000);

        AccessRequest request = new AccessRequest("a864f1e2c395@gwn", "sess_test12345678");
        request.setAuthProtocol(AccessRequest.AUTH_PAP);

        RadiusPacket response = client.authenticate(request);

        assertNotNull(response);
        assertEquals(RadiusPacket.ACCESS_ACCEPT, response.getPacketType());
        assertNotNull(response.getAttribute("Session-Timeout"));
        assertEquals("Haiphong GovTech Wi-Fi Access Granted", response.getAttributeValue("Reply-Message"));
    }

    @Test
    void testAccessRequest_AutoGrantHexMac_ReturnsAccessAccept() throws Exception {
        // For standard 12-char hex MAC address without prior session (free access)
        when(sessionRepository.findAll()).thenReturn(List.of());

        RadiusClient client = new RadiusClient("127.0.0.1", SHARED_SECRET);
        client.setAuthPort(TEST_AUTH_PORT);
        client.setSocketTimeout(3000);

        AccessRequest request = new AccessRequest("4ed3637409d3", "guest");
        request.setAuthProtocol(AccessRequest.AUTH_PAP);

        RadiusPacket response = client.authenticate(request);

        assertNotNull(response);
        assertEquals(RadiusPacket.ACCESS_ACCEPT, response.getPacketType());
        assertEquals("7200", response.getAttributeValue("Session-Timeout"));
    }
}

