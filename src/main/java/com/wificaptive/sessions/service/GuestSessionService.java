package com.wificaptive.sessions.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.GatewayException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.gateway.adapter.GatewayAdapter;
import com.wificaptive.gateway.dto.GatewayRevokeResult;
import com.wificaptive.sessions.dto.GuestSessionDto;
import com.wificaptive.sessions.entity.GuestSession;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class GuestSessionService {

    private static final Logger log = LoggerFactory.getLogger(GuestSessionService.class);

    private final GuestSessionRepository sessionRepository;
    private final GatewayAdapter gatewayAdapter;
    private final AuditService auditService;
    private final com.wificaptive.network.service.AccessLogRetentionService accessLogRetentionService;

    public GuestSessionService(
            GuestSessionRepository sessionRepository,
            GatewayAdapter gatewayAdapter,
            AuditService auditService,
            com.wificaptive.network.service.AccessLogRetentionService accessLogRetentionService) {
        this.sessionRepository = sessionRepository;
        this.gatewayAdapter = gatewayAdapter;
        this.auditService = auditService;
        this.accessLogRetentionService = accessLogRetentionService;
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

        for (GuestSession session : expiredSessions) {
            try {
                gatewayAdapter.revokeSession(
                        session.getId(),
                        session.getMacAddress(),
                        session.getIpAddress(),
                        session.getGatewayReference()
                );
                session.setStatus(SessionStatus.EXPIRED);
                sessionRepository.save(session);

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
}
