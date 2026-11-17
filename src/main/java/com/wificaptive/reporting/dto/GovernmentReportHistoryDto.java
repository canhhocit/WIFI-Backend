package com.wificaptive.reporting.dto;

import java.time.Instant;

public record GovernmentReportHistoryDto(
        Long id,
        String reportCode,
        String title,
        String period,
        Integer totalLocations,
        Integer onlineLocations,
        Double slaPercentage,
        Long totalSessions,
        Double totalDataGb,
        String exportFormat,
        String status,
        String submittedBy,
        Instant submittedAt,
        String recipientAgency,
        String notes,
        Instant createdAt
) {}
