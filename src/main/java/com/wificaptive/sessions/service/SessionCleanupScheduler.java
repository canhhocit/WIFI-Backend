package com.wificaptive.sessions.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupScheduler.class);

    private final GuestSessionService sessionService;

    public SessionCleanupScheduler(GuestSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void cleanupExpiredSessions() {
        try {
            int expired = sessionService.checkAndExpireSessions();
            if (expired > 0) {
                log.info("Session cleanup completed: {} sessions marked as EXPIRED", expired);
            }
        } catch (Exception e) {
            log.error("Failed during session expiration cleanup task: {}", e.getMessage(), e);
        }
    }
}
