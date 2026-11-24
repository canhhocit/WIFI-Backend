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

        verify(sessionRepository).saveAll(any());
    }

    @Test
    void testSyncActiveSessionsWithCloud_UpdatesTrafficAndExpiresDisconnectedOrMissingClients() {
        com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter cloudAdapter =
                mock(com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter.class);
        com.wificaptive.devices.mesh.MeshApMonitorService meshMonitor =
                mock(com.wificaptive.devices.mesh.MeshApMonitorService.class);

        GuestSessionService cloudSyncService = new GuestSessionService(
                sessionRepository, gatewayAdapter, auditService, accessLogRetentionService,
                cloudAdapter, meshMonitor, 15, 3);

        Instant now = Instant.now();

        // 1. Phiên thật đang online trên GWN Cloud -> phải giữ AUTHORIZED và cập nhật lưu lượng + hostname
        GuestSession onlineSession = new GuestSession();
        onlineSession.setId(66L);
        onlineSession.setMacAddress("a0-51-0b-30-8c-98");
        onlineSession.setGuestName("Guest User");
        onlineSession.setStatus(SessionStatus.AUTHORIZED);
        onlineSession.setAuthorizedAt(now.minusSeconds(1800));
        onlineSession.setExpiresAt(now.plusSeconds(5400));

        // 2. Phiên test giả không tồn tại trên Cloud (40001) quá grace period -> phải EXPIRED
        GuestSession ghostSession = new GuestSession();
        ghostSession.setId(65L);
        ghostSession.setMacAddress("02:4b:e0:42:62:48");
        ghostSession.setStatus(SessionStatus.AUTHORIZED);
        ghostSession.setAuthorizedAt(now.minusSeconds(1800));
        ghostSession.setExpiresAt(now.plusSeconds(5400));

        // 3. Phiên đã rời mạng quá 15 phút (idle 40 phút) -> phải EXPIRED
        GuestSession idleSession = new GuestSession();
        idleSession.setId(64L);
        idleSession.setMacAddress("5a-8a-27-70-52-15");
        idleSession.setStatus(SessionStatus.AUTHORIZED);
        idleSession.setAuthorizedAt(now.minusSeconds(3600));
        idleSession.setExpiresAt(now.plusSeconds(3600));

        // 4. Phiên khi API Cloud tạm lỗi mạng -> phải fail-safe giữ nguyên AUTHORIZED
        GuestSession unreachableSession = new GuestSession();
        unreachableSession.setId(67L);
        unreachableSession.setMacAddress("aa-bb-cc-dd-ee-ff");
        unreachableSession.setStatus(SessionStatus.AUTHORIZED);
        unreachableSession.setAuthorizedAt(now.minusSeconds(1800));
        unreachableSession.setExpiresAt(now.plusSeconds(5400));

        when(sessionRepository.findByStatusWithLocation(SessionStatus.AUTHORIZED))
                .thenReturn(List.of(onlineSession, ghostSession, idleSession, unreachableSession));
        when(cloudAdapter.isLiveCredentialsConfigured()).thenReturn(true);
        when(meshMonitor.getLiveConnectedClientMacs()).thenReturn(java.util.Set.of());

        when(cloudAdapter.fetchClientInfoFromCloud("a0-51-0b-30-8c-98"))
                .thenReturn(new com.wificaptive.gateway.dto.GwnClientLiveStatus(
                        true, true, false, "A0:51:0B:30:8C:98", "DESKTOP-GNQ6SIQ", "192.168.80.137",
                        "Windows", 41093914L, 59607655L, "EC:74:D7:98:B0:42", now.minusSeconds(120), true));
        when(cloudAdapter.fetchClientInfoFromCloud("02:4b:e0:42:62:48"))
                .thenReturn(com.wificaptive.gateway.dto.GwnClientLiveStatus.notFound("02:4B:E0:42:62:48"));
        when(cloudAdapter.fetchClientInfoFromCloud("5a-8a-27-70-52-15"))
                .thenReturn(new com.wificaptive.gateway.dto.GwnClientLiveStatus(
                        true, true, false, "5A:8A:27:70:52:15", "", "192.168.80.208",
                        "iOS", 8800805L, 4355120L, "EC:74:D7:98:B0:42", now.minusSeconds(2400), true));
        when(cloudAdapter.fetchClientInfoFromCloud("aa-bb-cc-dd-ee-ff"))
                .thenReturn(com.wificaptive.gateway.dto.GwnClientLiveStatus.unreachable("AA:BB:CC:DD:EE:FF"));

        var summary = cloudSyncService.syncActiveSessionsWithCloud("admin");

        assertTrue(summary.cloudConnected());
        assertEquals(4, summary.checkedCount());
        assertEquals(2, summary.onlineCount()); // onlineSession + unreachableSession (fail-safe)
        assertEquals(2, summary.expiredCount()); // ghostSession + idleSession
        assertEquals(1, summary.skippedCount()); // unreachableSession

        assertEquals(SessionStatus.AUTHORIZED, onlineSession.getStatus());
        assertEquals(41093914L, onlineSession.getBytesDownloaded());
        assertEquals(59607655L, onlineSession.getBytesUploaded());
        assertEquals("DESKTOP-GNQ6SIQ", onlineSession.getGuestName());

        assertEquals(SessionStatus.EXPIRED, ghostSession.getStatus());
        assertEquals(SessionStatus.EXPIRED, idleSession.getStatus());
        assertEquals(SessionStatus.AUTHORIZED, unreachableSession.getStatus());
        verify(sessionRepository).saveAll(any());
    }
}
