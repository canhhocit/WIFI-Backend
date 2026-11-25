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
            @Value("${app.gateway.gwn-cloud.client-idle-timeout-minutes:30}") int clientIdleTimeoutMinutes,
            @Value("${app.gateway.gwn-cloud.new-session-grace-minutes:15}") int newSessionGraceMinutes) {
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
                session.setRevokedAt(now);
                if (!StringUtils.hasText(session.getRevokeReason())) {
                    session.setRevokeReason("Hết thời gian sử dụng (TTL)");
                }
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
        List<GuestSession> recentExpired = sessionRepository.findUnexpiredByStatusWithLocation(SessionStatus.EXPIRED, now);

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

        if (activeSessions.isEmpty() && (recentExpired == null || recentExpired.isEmpty())) {
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
        int restoredCount = 0;
        List<GuestSession> modified = new java.util.ArrayList<>();
        Set<String> activeCleanMacs = new java.util.HashSet<>();

        for (GuestSession session : activeSessions) {
            Instant startedAt = session.getAuthorizedAt() != null ? session.getAuthorizedAt() : session.getCreatedAt();
            boolean pastGrace = startedAt == null || startedAt.isBefore(now.minus(gracePeriod));
            String normalizedMac = normalizeCleanMac(session.getMacAddress());
            boolean presentOnMesh = !normalizedMac.isEmpty() && meshLiveMacs.contains(normalizedMac);

            // 1. Nếu đã quá hạn TTL -> đánh dấu hết hạn ngay
            if (session.getExpiresAt() != null && !session.getExpiresAt().isAfter(now)) {
                session.setStatus(SessionStatus.EXPIRED);
                session.setRevokedAt(now);
                if (!StringUtils.hasText(session.getRevokeReason())) {
                    session.setRevokeReason("Hết thời gian sử dụng (TTL)");
                }
                modified.add(session);
                recordCompletionSafely(session, now, "EXPIRED");
                expiredCount++;
                continue;
            }

            if (!normalizedMac.isEmpty()) {
                activeCleanMacs.add(normalizedMac);
            }

            // 2. Truy vấn GWN Cloud /oapi/v1.0.0/client/info (theo networkId của Router gắn điểm nếu có)
            String routerNetworkId = resolveLocationRouterNetworkId(session);
            GwnClientLiveStatus live = StringUtils.hasText(routerNetworkId)
                    ? gwnCloudGatewayAdapter.fetchClientInfoFromCloud(session.getMacAddress(), routerNetworkId)
                    : gwnCloudGatewayAdapter.fetchClientInfoFromCloud(session.getMacAddress());
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
                    session.setRevokedAt(now);
                    session.setRevokeReason("Thiết bị đã rời mạng Wi-Fi");
                    modified.add(session);
                    recordCompletionSafely(session, now, "CLOUD_CLIENT_NOT_FOUND");
                    expiredCount++;
                    activeCleanMacs.remove(normalizedMac);
                    log.info("[Session Cloud Sync] Đóng phiên #{} (MAC: {}) vì không tồn tại trên GWN Cloud (40001)",
                            session.getId(), session.getMacAddress());
                }
                continue;
            }

            // 3. Client có trên GWN Cloud -> đồng bộ lưu lượng thực tế & tên thiết bị
            long prevTotalBytes = safeBytes(session.getBytesDownloaded()) + safeBytes(session.getBytesUploaded());
            boolean dirty = applyCloudClientMetrics(session, live);
            long newTotalBytes = safeBytes(session.getBytesDownloaded()) + safeBytes(session.getBytesUploaded());
            boolean trafficGrew = prevTotalBytes > 0 && newTotalBytes > prevTotalBytes;
            if (dirty) {
                updatedTrafficCount++;
            }

            // 4. Đánh giá thiết bị còn đang hoạt động thực tế hay đã rời mạng:
            // - GWN7002 với client sau Mesh/LAN (wired=1) đôi khi giữ nguyên lastactiveTime cũ trước thời điểm đăng nhập.
            //   Nếu lastActiveAt cũ hơn authorizedAt - 2 phút -> coi đó là dấu thời gian trước phiên, dựa vào baseline phiên & lưu lượng tăng.
            // - Nếu lưu lượng rx+tx vừa tăng thêm so với lần sync trước -> chắc chắn khách đang dùng mạng.
            boolean activeOnCloud = isClientActivelyUsingNetwork(live, session, startedAt, now, idleThreshold, trafficGrew);
            if (!activeOnCloud && !presentOnMesh && pastGrace) {
                session.setStatus(SessionStatus.EXPIRED);
                session.setRevokedAt(now);
                session.setRevokeReason(live.portalEnable()
                        ? "Không phát sinh lưu lượng (Quá giờ chờ)"
                        : "Thiết bị đã hết phiên trên Gateway");
                dirty = true;
                recordCompletionSafely(session, now, "CLOUD_IDLE_DISCONNECTED");
                expiredCount++;
                activeCleanMacs.remove(normalizedMac);
                log.info("[Session Cloud Sync] Đóng phiên #{} (MAC: {}) do đã ngừng hoạt động trên GWN Cloud (lastActive={}, portalEnable={})",
                        session.getId(), session.getMacAddress(), live.lastActiveAt(), live.portalEnable());
            } else {
                onlineCount++;
            }

            if (dirty) {
                modified.add(session);
            }
        }

        // 5. Tự động đối soát & khôi phục các phiên chưa hết hạn (expiresAt > now) bị đóng nhầm trước đó
        // nếu GWN Cloud xác nhận thiết bị vẫn đang Đã xác thực (portalEnable=1) và còn lưu lượng / hoạt động gần đây.
        if (recentExpired != null && !recentExpired.isEmpty()) {
            Set<String> seenExpiredMacs = new java.util.HashSet<>();
            for (GuestSession expiredCandidate : recentExpired) {
                String cleanMac = normalizeCleanMac(expiredCandidate.getMacAddress());
                if (cleanMac.isEmpty() || !seenExpiredMacs.add(cleanMac)) {
                    continue; // Chỉ xét phiên mới nhất của mỗi MAC
                }
                if (activeCleanMacs.contains(cleanMac)) {
                    continue; // MAC này đã có phiên AUTHORIZED mới hơn
                }
                if (isExplicitTerminationReason(expiredCandidate.getRevokeReason())) {
                    continue; // Không khôi phục phiên đã bị thay thế, tự ngắt hoặc hết TTL
                }

                Instant startedAt = expiredCandidate.getAuthorizedAt() != null
                        ? expiredCandidate.getAuthorizedAt()
                        : expiredCandidate.getCreatedAt();
                String routerNetworkId = resolveLocationRouterNetworkId(expiredCandidate);
                GwnClientLiveStatus live = StringUtils.hasText(routerNetworkId)
                        ? gwnCloudGatewayAdapter.fetchClientInfoFromCloud(expiredCandidate.getMacAddress(), routerNetworkId)
                        : gwnCloudGatewayAdapter.fetchClientInfoFromCloud(expiredCandidate.getMacAddress());

                if (!live.apiReachable() || !live.exists() || !live.portalEnable()) {
                    continue;
                }

                long prevTotalBytes = safeBytes(expiredCandidate.getBytesDownloaded()) + safeBytes(expiredCandidate.getBytesUploaded());
                long liveTotalBytes = safeBytes(live.rxBytes()) + safeBytes(live.txBytes());
                boolean trafficGrew = liveTotalBytes > prevTotalBytes;
                boolean presentOnMesh = meshLiveMacs.contains(cleanMac);
                boolean activeNow = live.isActiveWithin(now, idleThreshold)
                        || trafficGrew
                        || presentOnMesh
                        || isPreSessionStaleActivity(live.lastActiveAt(), startedAt, now, idleThreshold, expiredCandidate.getUpdatedAt());

                if (activeNow) {
                    applyCloudClientMetrics(expiredCandidate, live);
                    expiredCandidate.setStatus(SessionStatus.AUTHORIZED);
                    expiredCandidate.setRevokedAt(null);
                    expiredCandidate.setRevokeReason(null);
                    modified.add(expiredCandidate);
                    activeCleanMacs.add(cleanMac);
                    onlineCount++;
                    restoredCount++;
                    updatedTrafficCount++;
                    log.info("[Session Cloud Sync] Khôi phục phiên #{} (MAC: {}, tên: {}) về AUTHORIZED vì GWN Cloud xác nhận vẫn đang hoạt động (lastActive={}, rx={}, tx={})",
                            expiredCandidate.getId(), expiredCandidate.getMacAddress(), live.hostname(),
                            live.lastActiveAt(), live.rxBytes(), live.txBytes());
                }
            }
        }

        if (!modified.isEmpty()) {
            sessionRepository.saveAll(modified);
        }

        if (expiredCount > 0 || restoredCount > 0) {
            auditService.recordEvent(
                    AuditAction.SESSION_EXPIRE,
                    actorUsername != null ? actorUsername : "system-cloud-sync",
                    "internal",
                    "SESSION",
                    "CLOUD_SYNC",
                    String.format("Đồng bộ GWN Cloud: kiểm tra %d phiên, %d đang online (khôi phục %d phiên còn hoạt động), cập nhật lưu lượng %d, đóng %d phiên đã rời mạng/không tồn tại",
                            activeSessions.size(), onlineCount, restoredCount, updatedTrafficCount, expiredCount)
            );
        }

        String summaryMsg = restoredCount > 0
                ? String.format("Đã đồng bộ GWN Cloud: %d phiên đang online thật (đã khôi phục %d phiên còn hoạt động), cập nhật lưu lượng %d phiên, đóng %d phiên đã rời mạng.",
                        onlineCount, restoredCount, updatedTrafficCount, expiredCount)
                : String.format("Đã đồng bộ GWN Cloud: %d phiên đang online thật, cập nhật lưu lượng %d phiên, đóng %d phiên đã rời mạng/không tồn tại.",
                        onlineCount, updatedTrafficCount, expiredCount);
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

    private String normalizeCleanMac(String mac) {
        return mac != null ? mac.replaceAll("[^A-Fa-f0-9]", "").toUpperCase(Locale.ROOT) : "";
    }

    private long safeBytes(Long val) {
        return val != null ? Math.max(0L, val) : 0L;
    }

    private boolean isExplicitTerminationReason(String reason) {
        if (!StringUtils.hasText(reason)) return false;
        return reason.contains("thay thế")
                || reason.contains("ngắt")
                || reason.contains("disconnect")
                || reason.contains("TTL")
                || reason.contains("chặn");
    }

    private boolean applyCloudClientMetrics(GuestSession session, GwnClientLiveStatus live) {
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
        return dirty;
    }

    private boolean isPreSessionStaleActivity(
            Instant lastActiveAt,
            Instant startedAt,
            Instant now,
            Duration idleThreshold,
            Instant sessionUpdatedAt
    ) {
        if (startedAt == null || lastActiveAt == null) return false;
        // GWN7002 đôi khi giữ nguyên lastactiveTime cũ từ trước khi phiên Captive Portal bắt đầu
        if (lastActiveAt.isBefore(startedAt.minusSeconds(120))) {
            Instant baseline = (sessionUpdatedAt != null && sessionUpdatedAt.isAfter(startedAt))
                    ? sessionUpdatedAt
                    : startedAt;
            return baseline.isAfter(now.minus(idleThreshold));
        }
        return false;
    }

    private boolean isClientActivelyUsingNetwork(
            GwnClientLiveStatus live,
            GuestSession session,
            Instant startedAt,
            Instant now,
            Duration idleThreshold,
            boolean trafficGrew
    ) {
        if (trafficGrew) {
            return true;
        }
        if (isPreSessionStaleActivity(live.lastActiveAt(), startedAt, now, idleThreshold, session.getUpdatedAt())) {
            return true;
        }
        return live.isActiveWithin(now, idleThreshold);
    }

    /**
     * Thu hồi toàn bộ phiên đang chạy của MAC bị chặn, đóng trạng thái trong DB
     * và phát lệnh chặn/ngắt bypass xuống GWN Cloud ngay lập tức.
     */
    @Transactional
    public int enforceHardwareMacBlock(String macAddress, String actorUsername, String reason) {
        if (!StringUtils.hasText(macAddress)) {
            return 0;
        }
        String cleanMac = macAddress.replaceAll("[^A-Fa-f0-9]", "").toUpperCase(Locale.ROOT);
        if (cleanMac.isEmpty()) {
            return 0;
        }

        Instant now = Instant.now();
        String actor = StringUtils.hasText(actorUsername) ? actorUsername : "system";
        String effectiveReason = StringUtils.hasText(reason) ? reason : "Device MAC blocked by admin";

        List<GuestSession> activeSessions = sessionRepository.findAuthorizedByCleanMac(cleanMac);
        if (activeSessions != null && !activeSessions.isEmpty()) {
            for (GuestSession session : activeSessions) {
                try {
                    if (gatewayAdapter instanceof com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter gwnAdapter) {
                        com.wificaptive.devices.entity.NetworkDevice router = resolveLocationRouter(session);
                        gwnAdapter.revokeSessionForRouter(
                                session.getId(),
                                session.getMacAddress(),
                                session.getIpAddress(),
                                session.getGatewayReference(),
                                router != null ? router.getMacAddress() : null,
                                router != null ? router.getGwnNetworkId() : null
                        );
                    } else {
                        gatewayAdapter.revokeSession(
                                session.getId(),
                                session.getMacAddress(),
                                session.getIpAddress(),
                                session.getGatewayReference()
                        );
                    }
                } catch (Exception e) {
                    log.warn("[MAC Block] Không thể revoke phiên #{} trên gateway cho MAC {}: {}",
                            session.getId(), macAddress, e.getMessage());
                }
                session.setStatus(SessionStatus.REVOKED);
                session.setRevokedAt(now);
                session.setRevokedBy(actor);
                session.setRevokeReason(effectiveReason);
                recordCompletionSafely(session, now, "MAC_BLOCKED");
            }
            sessionRepository.saveAll(activeSessions);
        } else {
            // Phòng trường hợp bypass trên GWN Cloud còn sót dù DB không còn phiên AUTHORIZED
            try {
                gatewayAdapter.revokeSession(null, macAddress, null, "MANUAL_MAC_BLOCK");
            } catch (Exception e) {
                log.warn("[MAC Block] Lỗi gọi revoke bypass dự phòng cho MAC {}: {}", macAddress, e.getMessage());
            }
        }

        if (gwnCloudGatewayAdapter != null) {
            gwnCloudGatewayAdapter.blockMacAtHardwareLevel(macAddress);
        }

        int revokedCount = activeSessions != null ? activeSessions.size() : 0;
        log.info("[MAC Block] Đã đồng bộ chặn xuống GWN Cloud cho MAC {} (thu hồi {} phiên đang chạy)", macAddress, revokedCount);
        return revokedCount;
    }

    /**
     * Gỡ chặn MAC ở mức phần cứng trên GWN Cloud khi Admin bỏ chặn thiết bị.
     */
    public void releaseHardwareMacBlock(String macAddress) {
        if (!StringUtils.hasText(macAddress) || gwnCloudGatewayAdapter == null) {
            return;
        }
        gwnCloudGatewayAdapter.unblockMacAtHardwareLevel(macAddress);
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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wificaptive.devices.repository.NetworkDeviceRepository networkDeviceRepository;

    private com.wificaptive.devices.entity.NetworkDevice resolveLocationRouter(GuestSession session) {
        if (session == null || session.getLocation() == null || session.getLocation().getId() == null || networkDeviceRepository == null) {
            return null;
        }
        try {
            List<com.wificaptive.devices.entity.NetworkDevice> routers =
                    networkDeviceRepository.findByLocationIdAndDeviceType(session.getLocation().getId(), "ROUTER");
            if (routers == null || routers.isEmpty()) return null;
            return routers.stream()
                    .filter(r -> "ONLINE".equalsIgnoreCase(r.getStatus()))
                    .findFirst()
                    .orElse(routers.get(0));
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveLocationRouterNetworkId(GuestSession session) {
        com.wificaptive.devices.entity.NetworkDevice router = resolveLocationRouter(session);
        return router != null ? router.getGwnNetworkId() : null;
    }
}
