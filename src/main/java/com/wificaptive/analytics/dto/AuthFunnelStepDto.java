package com.wificaptive.analytics.dto;

public record AuthFunnelStepDto(
        int step,
        String stepName,
        long count,
        double retentionPct,
        double dropPct
) {}
