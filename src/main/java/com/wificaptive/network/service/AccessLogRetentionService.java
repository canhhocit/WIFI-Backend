package com.wificaptive.network.service;

import com.wificaptive.network.dto.AccessLogRetentionDto;
import com.wificaptive.network.entity.AccessLogRetention;
import com.wificaptive.network.repository.AccessLogRetentionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class AccessLogRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AccessLogRetentionService.class);

    private final AccessLogRetentionRepository retentionRepository;

    public AccessLogRetentionService(AccessLogRetentionRepository retentionRepository) {
        this.retentionRepository = retentionRepository;
    }

    public AccessLogRetention recordSessionCompletion(
            Long sessionId,
            String sessionToken,
            String clientMac,
            String clientIp,
            Long locationId,
            String locationName,
            String phoneNumber,
            Instant authenticatedAt,
            Instant disconnectedAt,
            Long durationSeconds,
            Long bytesIn,
            Long bytesOut,
            String terminationReason
    ) {
        AccessLogRetention record = new AccessLogRetention();
        record.setSessionId(sessionId);
        record.setSessionToken(sessionToken);
        record.setClientMac(clientMac != null ? clientMac.toUpperCase() : "UNKNOWN");
        record.setClientIp(clientIp != null ? clientIp : "0.0.0.0");
        record.setLocationId(locationId);
        record.setLocationName(locationName);
        record.setPhoneNumber(phoneNumber);
        record.setAuthenticatedAt(authenticatedAt != null ? authenticatedAt : Instant.now());
        record.setDisconnectedAt(disconnectedAt != null ? disconnectedAt : Instant.now());
        record.setDurationSeconds(durationSeconds != null ? durationSeconds : 0L);
        record.setBytesIn(bytesIn != null ? bytesIn : 0L);
        record.setBytesOut(bytesOut != null ? bytesOut : 0L);
        record.setTerminationReason(terminationReason != null ? terminationReason : "USER_LOGOUT");

        // Legal 12-month retention requirement: retain for at least 365 days
        record.setRetainedUntil(record.getAuthenticatedAt().plus(365, ChronoUnit.DAYS));

        AccessLogRetention saved = retentionRepository.save(record);
        log.info("Recorded 12-month legal access log id {} for session {} (MAC: {}, retained until {})",
                saved.getId(), sessionId, clientMac, saved.getRetainedUntil());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AccessLogRetentionDto> searchLogs(
            String clientMac,
            Long locationId,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        String cleanMac = (clientMac != null && !clientMac.isBlank()) ? clientMac.trim() : null;
        Instant effectiveStart = startDate != null ? startDate : Instant.ofEpochMilli(0L);
        Instant effectiveEnd = endDate != null ? endDate : Instant.now().plus(3650, ChronoUnit.DAYS);
        return retentionRepository.searchLogs(cleanMac, locationId, effectiveStart, effectiveEnd, pageable)
                .map(AccessLogRetentionDto::fromEntity);
    }

    /**
     * Nightly scheduled purge at 03:00 AM UTC for records exceeding 12 months retention window
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredRetentionLogs() {
        Instant now = Instant.now();
        List<AccessLogRetention> expired = retentionRepository.findByRetainedUntilBefore(now);
        if (!expired.isEmpty()) {
            retentionRepository.deleteAll(expired);
            log.info("Purged {} expired access log records older than 12 months legal retention window.", expired.size());
        }
    }
}

