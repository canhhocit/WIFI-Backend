package com.wificaptive.network.dto;

import com.wificaptive.network.entity.MacBlacklist;
import java.time.Instant;

public record MacBlacklistDto(
        Long id,
        String macAddress,
        String reason,
        String blockedBy,
        Instant blockedAt,
        Boolean active
) {
    public static MacBlacklistDto fromEntity(MacBlacklist b) {
        return new MacBlacklistDto(
                b.getId(),
                b.getMacAddress(),
                b.getReason(),
                b.getBlockedBy(),
                b.getBlockedAt(),
                b.getActive()
        );
    }
}
