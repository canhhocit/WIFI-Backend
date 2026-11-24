package com.wificaptive.sessions.dto;

import java.time.Instant;

public record SessionSyncSummaryDto(
        int checkedCount,
        int onlineCount,
        int updatedTrafficCount,
        int expiredCount,
        int skippedCount,
        boolean cloudConnected,
        Instant syncedAt,
        String message
) {}
