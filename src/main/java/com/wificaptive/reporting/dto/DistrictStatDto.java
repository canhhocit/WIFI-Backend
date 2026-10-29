package com.wificaptive.reporting.dto;

public record DistrictStatDto(
        Long districtId,
        String districtName,
        int totalLocations,
        int onlineLocations,
        double availabilityPercent,
        long totalSessions,
        double totalDataGb,
        int totalIncidents,
        double slaCompliancePercent
) {}
