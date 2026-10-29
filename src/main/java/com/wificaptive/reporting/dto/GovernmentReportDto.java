package com.wificaptive.reporting.dto;

import java.time.Instant;
import java.util.List;

public record GovernmentReportDto(
        String reportPeriod,
        String cityTitle,
        String authorityName,
        Instant generatedAt,
        int totalTargetLocations,
        int activeLocations,
        double networkAvailabilityRate,
        long totalCitizenSessions,
        double totalDataServedTb,
        double citizenSatisfactionRate,
        int totalIncidentsReported,
        double telecomSlaComplianceRate,
        double averageResolutionHours,
        List<DistrictStatDto> districtBreakdown
) {}

