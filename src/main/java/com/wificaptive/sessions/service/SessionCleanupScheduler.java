package com.wificaptive.sessions.service;

import com.wificaptive.common.lock.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SessionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupScheduler.class);

    private final GuestSessionService sessionService;
    private final DistributedLockService lockService;

    public SessionCleanupScheduler(GuestSessionService sessionService,
                                   DistributedLockService lockService) {
        this.sessionService = sessionService;
        this.lockService = lockService;
    }

    public SessionCleanupScheduler(GuestSessionService sessionService) {
        this(sessionService, new DistributedLockService(null));
    }

    // Chạy dọn dẹp mỗi 60 giây (thay vì 5 giây) để tránh khóa bảng và giảm tải I/O Database
    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void cleanupExpiredSessions() {
        lockService.runWithLock("scheduler:session-cleanup", Duration.ofSeconds(50), () -> {
            try {
                int expired = sessionService.checkAndExpireSessions();
                if (expired > 0) {
                    log.info("Session cleanup completed: {} sessions marked as EXPIRED", expired);
                }
            } catch (Exception e) {
                log.error("Failed during session expiration cleanup task: {}", e.getMessage(), e);
            }
        });
    }

    // Đồng bộ phiên kết nối thực tế từ GWN Cloud & Mesh AP mỗi 2 phút
    @Scheduled(fixedDelayString = "${app.gateway.gwn-cloud.session-sync-interval-ms:120000}", initialDelay = 45000)
    public void syncActiveSessionsFromCloud() {
        lockService.runWithLock("scheduler:session-cloud-sync", Duration.ofSeconds(100), () -> {
            try {
                var summary = sessionService.syncActiveSessionsWithCloud("scheduler");
                if (summary.cloudConnected() && (summary.expiredCount() > 0 || summary.updatedTrafficCount() > 0)) {
                    log.info("[SessionCleanupScheduler] {}", summary.message());
                }
            } catch (Exception e) {
                log.warn("[SessionCleanupScheduler] Lỗi đồng bộ phiên kết nối từ GWN Cloud: {}", e.getMessage());
            }
        });
    }
}
