package com.wificaptive.sessions;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.gateway.adapter.GatewayAdapter;
import com.wificaptive.gateway.dto.GatewayRevokeResult;
import com.wificaptive.sessions.dto.GuestSessionDto;
import com.wificaptive.sessions.entity.GuestSession;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import com.wificaptive.sessions.service.GuestSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestSessionServiceTest {

    @Mock
    private GuestSessionRepository sessionRepository;

    @Mock
    private GatewayAdapter gatewayAdapter;

    @Mock
    private AuditService auditService;

    @Mock
    private com.wificaptive.network.service.AccessLogRetentionService accessLogRetentionService;

    private GuestSessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new GuestSessionService(sessionRepository, gatewayAdapter, auditService, accessLogRetentionService);
    }

    @Test
    void testRevokeSession_Success() {
        GuestSession session = new GuestSession();
        session.setId(10L);
        session.setSessionToken("sess_abc");
        session.setMacAddress("00:aa:bb:cc:dd:ee");
        session.setIpAddress("192.168.1.55");
        session.setGatewayReference("MOCK-REF-10");
        session.setStatus(SessionStatus.AUTHORIZED);

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(gatewayAdapter.revokeSession(10L, "00:aa:bb:cc:dd:ee", "192.168.1.55", "MOCK-REF-10"))
                .thenReturn(GatewayRevokeResult.success(true, "Revoked", 5));
        when(sessionRepository.save(any(GuestSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GuestSessionDto result = sessionService.revokeSession(10L, "Violation of terms", "admin");

        assertNotNull(result);
        assertEquals(SessionStatus.REVOKED, result.getStatus());
        assertEquals("Violation of terms", result.getRevokeReason());
        assertEquals("admin", result.getRevokedBy());

        verify(gatewayAdapter).revokeSession(10L, "00:aa:bb:cc:dd:ee", "192.168.1.55", "MOCK-REF-10");
    }

    @Test
    void testRevokeSession_AlreadyRevoked_ThrowsBadRequest() {
        GuestSession session = new GuestSession();
        session.setId(11L);
        session.setStatus(SessionStatus.REVOKED);

        when(sessionRepository.findById(11L)).thenReturn(Optional.of(session));

        assertThrows(BadRequestException.class, () ->
                sessionService.revokeSession(11L, "Double revoke attempt", "admin"));

        verifyNoInteractions(gatewayAdapter);
    }

    @Test
    void testCheckAndExpireSessions_PassivatesExpiredSessions() {
        GuestSession session1 = new GuestSession();
        session1.setId(21L);
        session1.setMacAddress("00:11:22:33:44:01");
        session1.setStatus(SessionStatus.AUTHORIZED);
        session1.setExpiresAt(Instant.now().minusSeconds(100));

        GuestSession session2 = new GuestSession();
        session2.setId(22L);
        session2.setMacAddress("00:11:22:33:44:02");
        session2.setStatus(SessionStatus.AUTHORIZED);
        session2.setExpiresAt(Instant.now().minusSeconds(200));

        when(sessionRepository.findSessionsToPassivate(eq(SessionStatus.AUTHORIZED), any(Instant.class)))
                .thenReturn(List.of(session1, session2));

        when(gatewayAdapter.revokeSession(any(), any(), any(), any()))
                .thenReturn(GatewayRevokeResult.success(true, "Rule removed", 5));

        int expiredCount = sessionService.checkAndExpireSessions();

        assertEquals(2, expiredCount);
        assertEquals(SessionStatus.EXPIRED, session1.getStatus());
        assertEquals(SessionStatus.EXPIRED, session2.getStatus());

        verify(sessionRepository, times(2)).save(any(GuestSession.class));
    }
}
