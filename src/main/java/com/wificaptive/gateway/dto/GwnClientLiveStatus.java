package com.wificaptive.gateway.dto;

import java.time.Duration;
import java.time.Instant;

/**
 * Trạng thái kết nối thời gian thực của một client truy vấn từ Grandstream GWN Cloud (/oapi/v1.0.0/client/info).
 */
public record GwnClientLiveStatus(
        boolean apiReachable,
        boolean exists,
        boolean notFound,
        String mac,
        String hostname,
        String ipv4,
        String dhcpOs,
        long rxBytes,
        long txBytes,
        String apId,
        Instant lastActiveAt,
        boolean wired,
        boolean portalEnable
) {
    /** Constructor giữ tương thích cho các unit test 12 tham số cũ (mặc định portalEnable = true nếu client tồn tại). */
    public GwnClientLiveStatus(
            boolean apiReachable,
            boolean exists,
            boolean notFound,
            String mac,
            String hostname,
            String ipv4,
            String dhcpOs,
            long rxBytes,
            long txBytes,
            String apId,
            Instant lastActiveAt,
            boolean wired
    ) {
        this(apiReachable, exists, notFound, mac, hostname, ipv4, dhcpOs, rxBytes, txBytes, apId, lastActiveAt, wired, exists);
    }

    public static GwnClientLiveStatus notFound(String mac) {
        return new GwnClientLiveStatus(true, false, true, mac, null, null, null, 0L, 0L, null, null, false, false);
    }

    public static GwnClientLiveStatus unreachable(String mac) {
        return new GwnClientLiveStatus(false, false, false, mac, null, null, null, 0L, 0L, null, null, false, false);
    }

    public boolean isActiveWithin(Instant now, Duration idleThreshold) {
        if (!exists) {
            return false;
        }
        if (lastActiveAt == null) {
            return true;
        }
        return lastActiveAt.isAfter(now.minus(idleThreshold));
    }
}
