package com.wificaptive.gateway.dto;

import java.util.List;

public record GwnUpgradeResult(
        boolean success,
        String message,
        List<String> upgradedMacs,
        String targetFirmware,
        long elapsedMs
) {
    public static GwnUpgradeResult success(String message, List<String> macs, String firmware, long elapsedMs) {
        return new GwnUpgradeResult(true, message, macs, firmware, elapsedMs);
    }

    public static GwnUpgradeResult failure(String message, String firmware, long elapsedMs) {
        return new GwnUpgradeResult(false, message, List.of(), firmware, elapsedMs);
    }
}
