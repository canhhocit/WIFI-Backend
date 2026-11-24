package com.wificaptive.sessions.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.GatewayException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.devices.mesh.MeshApMonitorService;
import com.wificaptive.gateway.adapter.GatewayAdapter;
import com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter;
import com.wificaptive.gateway.dto.GatewayRevokeResult;
import com.wificaptive.gateway.dto.GwnClientLiveStatus;
import com.wificaptive.sessions.dto.GuestSessionDto;
import com.wificaptive.sessions.dto.SessionSyncSummaryDto;
import com.wificaptive.sessions.entity.GuestSession;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GuestSessionService {

    private static final Logger log = LoggerFactory.getLogger(GuestSessionService.class);

    private final GuestSessionRepository sessionRepository;
    private final GatewayAdapter gatewayAdapter;
    private final AuditService auditService;
    private final com.wificaptive.network.service.AccessLogRetentionService accessLogRetentionService;
    private final GwnCloudGatewayAdapter gwnCloudGatewayAdapter;
    private final MeshApMonitorService meshApMonitorService;
    private final int clientIdleTimeoutMinutes;
    private final int newSessionGraceMinutes;

    public GuestSessionService(
            GuestSessionRepository sessionRepository,
            GatewayAdapter gatewayAdapter,
            AuditService auditService,
            com.wificaptive.network.service.AccessLogRetentionService accessLogRetentionService) {
        this(sessionRepository, gatewayAdapter, auditService, accessLogRetentionService, null, null, 15, 3);
    }

    @Autowired
    public GuestSessionService(
            GuestSessionRepository sessionRepository,
            GatewayAdapter gatewayAdapter,
            AuditService auditService,
            com.wificaptive.network.service.AccessLogRetentionService accessLogRetentionService,
            @Autowired(required = false) GwnCloudGatewayAdapter gwnCloudGatewayAdapter,
            @Autowired(required = false) MeshApMonitorService meshApMonitorService,
            @Value("${app.gateway.gwn-cloud.client-idle-timeout-minutes:15}") int clientIdleTimeoutMinutes,
            @Value("${app.gateway.gwn-cloud.new-session-grace-minutes:3}") int newSessionGraceMinutes) {
        this.sessionRepository = sessionRepository;
        this.gatewayAdapter = gatewayAdapter;
        this.auditService = auditService;
        this.accessLogRetentionService = accessLogRetentionService;
        this.gwnCloudGatewayAdapter = gwnCloudGatewayAdapter;
        this.meshApMonitorService = meshApMonitorService;
        this.clientIdleTimeoutMinutes = Math.max(5, clientIdleTimeoutMinutes);
        this.newSessionGraceMinutes = Math.max(1, newSessionGraceMinutes);
    }

    @Transactional(readOnly = true)
    public PageResponse<GuestSessionDto> getSessions(Long locationId, SessionStatus status, String search, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<GuestSession> page;
        if (cleanSearch == null && locationId == null && status == null) {
            page = sessionRepository.findAllWithLocation(pageable);
        } else {
            page = sessionRepository.findFiltered(locationId, status, cleanSearch, pageable);
        }
        return PageResponse.from(page.map(GuestSessionDto::from));
    }

    @Transactional(readOnly = true)
    public GuestSessionDto getSessionById(Long id) {
        GuestSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GuestSession", "id", id));
        return GuestSessionDto.from(session);
    }

    @Transactional(readOnly = true)
    public GuestSessionDto getSessionByToken(String token) {
        GuestSession session = sessionRepository.findBySessionToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("GuestSession", "token", token));
        return GuestSessionDto.from(session);
    }

    @Transactional
    public GuestSessionDto revokeSession(Long id, String reason, String actorUsername) {
        GuestSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GuestSession", "id", id));

        if (session.getStatus() != SessionStatus.AUTHORIZED && session.getStatus() != SessionStatus.PENDING) {
            throw new BadRequestException("Cannot revoke session. Current status is " + session.getStatus());
        }

        log.info("Revoking session {} (MAC: {}, IP: {}) requested by {}", session.getId(), session.getMacAddress(), session.getIpAddress(), actorUsername);

        // Instruct gateway to drop network pass rule
        GatewayRevokeResult revokeResult = gatewayAdapter.revokeSession(
                session.getId(),
                session.getMacAddress(),
                session.getIpAddress(),
                session.getGatewayReference()
        );

        if (!revokeResult.isSuccess()) {
            throw new GatewayException("Failed to revoke session on gateway: " + revokeResult.getMessage(), revokeResult.getStatusCode());
        }

        session.setStatus(SessionStatus.REVOKED);
        session.setRevokedAt(Instant.now());
        session.setRevokedBy(actorUsername);
        session.setRevokeReason(reason);

        GuestSession updated = sessionRepository.save(session);

        if (accessLogRetentionService != null) {
            Instant start = session.getAuthorizedAt() != null ? session.getAuthorizedAt() : session.getCreatedAt();
            long duration = start != null
                    ? Instant.now().getEpochSecond() - start.getEpochSecond()
                    : 0;
            accessLogRetentionService.recordSessionCompletion(
                    session.getId(),
                    session.getSessionToken(),
                    session.getMacAddress(),
                    session.getIpAddress(),
                    session.getLocation() != null ? session.getLocation().getId() : null,
                    session.getLocation() != null ? session.getLocation().getName() : null,
                    session.getGuestContact(),
                    start,
                    Instant.now(),
                    duration,
                    session.getBytesDownloaded(),
                    session.getBytesUploaded(),
                    "ADMIN_REVOKED"
            );
        }

        auditService.recordEvent(
                AuditAction.SESSION_REVOKE,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "SESSION",
                session.getId().toString(),
                String.format("Revoked session token %s for MAC %s. Reason: %s",
                        session.getSessionToken(), session.getMacAddress(), reason)
        );

        return GuestSessionDto.from(updated);
    }

    @Transactional
    public int checkAndExpireSessions() {
        Instant now = Instant.now();
        List<GuestSession> expiredSessions = sessionRepository.findSessionsToPassivate(SessionStatus.AUTHORIZED, now);

        if (expiredSessions.isEmpty()) {
            return 0;
        }

        log.info("Processing {} expired guest sessions...", expiredSessions.size());
        int count = 0;
        List<GuestSession> toUpdate = new java.util.ArrayList<>();

        for (GuestSession session : expiredSessions) {
            try {
                gatewayAdapter.revokeSession(
                        session.getId(),
                        session.getMacAddress(),
                        session.getIpAddress(),
                        session.getGatewayReference()
                );
                session.setStatus(SessionStatus.EXPIRED);
                toUpdate.add(session);

                if (accessLogRetentionService != null) {
                    Instant start = session.getAuthorizedAt() != null ? session.getAuthorizedAt() : session.getCreatedAt();
                    long duration = start != null
                            ? now.getEpochSecond() - start.getEpochSecond()
                            : 0;
                    accessLogRetentionService.recordSessionCompletion(
                            session.getId(),
                            session.getSessionToken(),
                            session.getMacAddress(),
                            session.getIpAddress(),
                            session.getLocation() != null ? session.getLocation().getId() : null,
                            session.getLocation() != null ? session.getLocation().getName() : null,
                            session.getGuestContact(),
                            start,
                            now,
                            duration,
                            session.getBytesDownloaded(),
                            session.getBytesUploaded(),
                            "EXPIRED"
                    );
                }

                count++;
            } catch (Exception e) {
                log.error("Error expiring session {}: {}", session.getId(), e.getMessage());
            }
        }

        if (!toUpdate.isEmpty()) {
            sessionRepository.saveAll(toUpdate);
        }

        if (count > 0) {
            auditService.recordEvent(
                    AuditAction.SESSION_EXPIRE,
                    "system-scheduler",
                    "127.0.0.1",
                    "SYSTEM",
                    "BATCH",
                    "Expired " + count + " guest sessions reaching TTL limit"
            );
        }

        return count;
    }

    /**
     * Đồng bộ các phiên đang kết nối (AUTHORIZED) với dữ liệu thực tế từ Grandstream GWN Cloud
     * (/oapi/v1.0.0/client/info) và Mesh AP live snapshot:
     * - Cập nhật lưu lượng thực tế (bytesDownloaded / bytesUploaded) & tên máy (hostname).
     * - Tự động đóng phiên (EXPIRED) nếu thiết bị không tồn tại trên router (retCode 40001)
     *   hoặc đã ngừng hoạt động vượt ngưỡng idle timeout.
     * - An toàn tuyệt đối (fail-safe): nếu GWN Cloud lỗi mạng/không truy cập được thì giữ nguyên phiên.
     */
    @Transactional
    public SessionSyncSummaryDto syncActiveSessionsWithCloud(String actorUsername) {
        Instant now = Instant.now();
        List<GuestSession> activeSessions = sessionRepository.findByStatusWithLocation(SessionStatus.AUTHORIZED);

        if (gwnCloudGatewayAdapter == null || !gwnCloudGatewayAdapter.isLiveCredentialsConfigured()) {
            return new SessionSyncSummaryDto(
                    activeSessions.size(),
                    activeSessions.size(),
                    0,
                    0,
                    activeSessions.size(),
                    false,
                    now,
                    "Chưa cấu hình GWN Cloud App ID/Secret nên không thể đồng bộ từ Cloud."
            );
        }

        if (activeSessions.isEmpty()) {
            return new SessionSyncSummaryDto(
                    0, 0, 0, 0, 0, true, now,
                    "Hiện không có phiên nào đang kết nối cần đối chiếu với GWN Cloud."
            );
        }

        Set<String> meshLiveMacs = meshApMonitorService != null
                ? meshApMonitorService.getLiveConnectedClientMacs()
                : Set.of();

        Duration idleThreshold = Duration.ofMinutes(clientIdleTimeoutMinutes);
        Duration gracePeriod = Duration.ofMinutes(newSessionGraceMinutes);

        int onlineCount = 0;
        int updatedTrafficCount = 0;
        int expiredCount = 0;
        int skippedCount = 0;
        List<GuestSession> modified = new java.util.ArrayList<>();

        for (GuestSession session : activeSessions) {
            Instant startedAt = session.getAuthorizedAt() != null ? session.getAuthorizedAt() : session.getCreatedAt();
            boolean pastGrace = startedAt == null || startedAt.isBefore(now.minus(gracePeriod));
            String normalizedMac = session.getMacAddress() != null
                    ? session.getMacAddress().replaceAll("[^A-Fa-f0-9]", "").toUpperCase(Locale.ROOT)
                    : "";
            boolean presentOnMesh = !normalizedMac.isEmpty() && meshLiveMacs.contains(normalizedMac);

            // 1. Nếu đã quá hạn TTL -> đánh dấu hết hạn ngay
            if (session.getExpiresAt() != null && !session.getExpiresAt().isAfter(now)) {
                session.setStatus(SessionStatus.EXPIRED);
                modified.add(session);
                recordCompletionSafely(session, now, "EXPIRED");
                expiredCount++;
                continue;
            }

            // 2. Truy vấn GWN Cloud /oapi/v1.0.0/client/info
            GwnClientLiveStatus live = gwnCloudGatewayAdapter.fetchClientInfoFromCloud(session.getMacAddress());
            if (!live.apiReachable()) {
                skippedCount++;
                onlineCount++;
                continue;
            }

            if (live.notFound()) {
                if (presentOnMesh || !pastGrace) {
                    onlineCount++;
                } else {
                    session.setStatus(SessionStatus.EXPIRED);
                    modified.add(session);
                    recordCompletionSafely(session, now, "CLOUD_CLIENT_NOT_FOUND");
                    expiredCount++;
                    log.info("[Session Cloud Sync] Đóng phiên #{} (MAC: {}) vì không tồn tại trên GWN Cloud (40001)",
                            session.getId(), session.getMacAddress());
                }
                continue;
            }

            // 3. Client có trên GWN Cloud -> đồng bộ lưu lượng thực tế & tên thiết bị
            boolean dirty = false;
            if (live.rxBytes() > 0 && !Long.valueOf(live.rxBytes()).equals(session.getBytesDownloaded())) {
                session.setBytesDownloaded(live.rxBytes());
                dirty = true;
            }
            if (live.txBytes() > 0 && !Long.valueOf(live.txBytes()).equals(session.getBytesUploaded())) {
                session.setBytesUploaded(live.txBytes());
                dirty = true;
            }
            if (StringUtils.hasText(live.hostname())
                    && (!StringUtils.hasText(session.getGuestName()) || "Guest User".equalsIgnoreCase(session.getGuestName().trim()))) {
                session.setGuestName(live.hostname().trim());
                dirty = true;
            }
            if (dirty) {
                updatedTrafficCount++;
            }

            // 4. Kiểm tra thiết bị còn hoạt động trong ngưỡng idle hay đã rời khỏi mạng
            boolean activeOnCloud = live.isActiveWithin(now, idleThreshold);
            if (!activeOnCloud && !presentOnMesh && pastGrace) {
                session.setStatus(SessionStatus.EXPIRED);
                dirty = true;
                recordCompletionSafely(session, now, "CLOUD_IDLE_DISCONNECTED");
                expiredCount++;
                log.info("[Session Cloud Sync] Đóng phiên #{} (MAC: {}) do đã ngừng hoạt động trên GWN Cloud (lastActive={})",
                        session.getId(), session.getMacAddress(), live.lastActiveAt());
            } else {
                onlineCount++;
            }

            if (dirty) {
                modified.add(session);
            }
        }

        if (!modified.isEmpty()) {
            sessionRepository.saveAll(modified);
        }

        if (expiredCount > 0) {
            auditService.recordEvent(
                    AuditAction.SESSION_EXPIRE,
                    actorUsername != null ? actorUsername : "system-cloud-sync",
                    "internal",
                    "SESSION",
                    "CLOUD_SYNC",
                    String.format("Đồng bộ GWN Cloud: kiểm tra %d phiên, %d đang online, cập nhật lưu lượng %d, đóng %d phiên đã rời mạng/không tồn tại",
                            activeSessions.size(), onlineCount, updatedTrafficCount, expiredCount)
            );
        }

        String summaryMsg = String.format(
                "Đã đồng bộ GWN Cloud: %d phiên đang online thật, cập nhật lưu lượng %d phiên, đóng %d phiên đã rời mạng/không tồn tại.",
                onlineCount, updatedTrafficCount, expiredCount
        );
        return new SessionSyncSummaryDto(
                activeSessions.size(),
                onlineCount,
                updatedTrafficCount,
                expiredCount,
                skippedCount,
                true,
                now,
                summaryMsg
        );
    }

    private void recordCompletionSafely(GuestSession session, Instant endedAt, String reason) {
        if (accessLogRetentionService == null) {
            return;
        }
        try {
            Instant start = session.getAuthorizedAt() != null ? session.getAuthorizedAt() : session.getCreatedAt();
            long duration = start != null ? Math.max(0L, endedAt.getEpochSecond() - start.getEpochSecond()) : 0L;
            accessLogRetentionService.recordSessionCompletion(
                    session.getId(),
                    session.getSessionToken(),
                    session.getMacAddress(),
                    session.getIpAddress(),
                    session.getLocation() != null ? session.getLocation().getId() : null,
                    session.getLocation() != null ? session.getLocation().getName() : null,
                    session.getGuestContact(),
                    start,
                    endedAt,
                    duration,
                    session.getBytesDownloaded(),
                    session.getBytesUploaded(),
                    reason
            );
        } catch (Exception e) {
            log.warn("Không thể ghi access log completion cho phiên {}: {}", session.getId(), e.getMessage());
        }
    }
}
