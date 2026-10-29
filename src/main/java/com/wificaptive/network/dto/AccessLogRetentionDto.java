package com.wificaptive.network.dto;

import com.wificaptive.network.entity.AccessLogRetention;
import java.time.Instant;

public record AccessLogRetentionDto(
        Long id,
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
        String terminationReason,
        Instant retainedUntil,
        Instant createdAt
) {
    public static AccessLogRetentionDto fromEntity(AccessLogRetention log) {
        return new AccessLogRetentionDto(
                log.getId(),
                log.getSessionId(),
                log.getSessionToken(),
                log.getClientMac(),
                log.getClientIp(),
                log.getLocationId(),
                log.getLocationName(),
                log.getPhoneNumber(),
                log.getAuthenticatedAt(),
                log.getDisconnectedAt(),
                log.getDurationSeconds(),
                log.getBytesIn(),
                log.getBytesOut(),
                log.getTerminationReason(),
                log.getRetainedUntil(),
                log.getCreatedAt()
        );
    }
}

