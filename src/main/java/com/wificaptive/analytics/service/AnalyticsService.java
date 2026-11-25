package com.wificaptive.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.analytics.dto.AuthFunnelStepDto;
import com.wificaptive.analytics.dto.DashboardOverviewDto;
import com.wificaptive.analytics.dto.DeviceTopologySummaryDto;
import com.wificaptive.analytics.dto.HourlyTrendDto;
import com.wificaptive.analytics.dto.LocationAnalyticsDto;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.DeviceRepository;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.gateway.adapter.GatewayAdapter;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AnalyticsService {

    private final GuestSessionRepository sessionRepository;
    private final WifiLocationRepository locationRepository;
    private final DeviceRepository deviceRepository;
    private final GatewayAdapter gatewayAdapter;
    private final NetworkDeviceRepository networkDeviceRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private volatile CachedOverview cachedOverview;
    private static final long OVERVIEW_CACHE_TTL_MS = 15000; // 15s cache protects database under concurrent admin dashboard traffic

    private static class CachedOverview {
        final DashboardOverviewDto dto;
        final long timestamp;
        CachedOverview(DashboardOverviewDto dto, long timestamp) {
            this.dto = dto;
            this.timestamp = timestamp;
        }
        boolean isValid() {
            return (System.currentTimeMillis() - timestamp) < OVERVIEW_CACHE_TTL_MS;
        }
    }

    public AnalyticsService(
            GuestSessionRepository sessionRepository,
            WifiLocationRepository locationRepository,
            DeviceRepository deviceRepository,
            GatewayAdapter gatewayAdapter,
            NetworkDeviceRepository networkDeviceRepository,
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.locationRepository = locationRepository;
        this.deviceRepository = deviceRepository;
        this.gatewayAdapter = gatewayAdapter;
        this.networkDeviceRepository = networkDeviceRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public AnalyticsService(
            GuestSessionRepository sessionRepository,
            WifiLocationRepository locationRepository,
            DeviceRepository deviceRepository,
            GatewayAdapter gatewayAdapter,
            NetworkDeviceRepository networkDeviceRepository) {
        this(sessionRepository, locationRepository, deviceRepository, gatewayAdapter, networkDeviceRepository, null, null);
    }

    @Transactional(readOnly = true)
    public DashboardOverviewDto getOverview() {
        if (redisTemplate != null && objectMapper != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get("cache:analytics:overview");
                if (cachedJson != null && !cachedJson.isBlank()) {
                    return objectMapper.readValue(cachedJson, DashboardOverviewDto.class);
                }
            } catch (Exception ignored) {
                // Redis error fallback to local cache
            }
        }

        CachedOverview currentCache = this.cachedOverview;
        if (currentCache != null && currentCache.isValid()) {
            return currentCache.dto;
        }

        Instant now = Instant.now();
        java.time.ZoneId vnZone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        Instant startOfToday = java.time.LocalDate.now(vnZone).atStartOfDay(vnZone).toInstant();

        long totalSessions = sessionRepository.count();
        long activeSessions = sessionRepository.countActiveSessions(now);
        long todaySessions = sessionRepository.countSessionsSince(startOfToday);
        long totalLocations = locationRepository.countByDeletedFalse();
        Set<Long> onlineRouterLocationIds = networkDeviceRepository.findOnlineRouterLocationIds();
        long activeLocations = locationRepository.findByActiveTrueAndDeletedFalse().stream()
                .filter(loc -> onlineRouterLocationIds.contains(loc.getId()))
                .count();
        long totalDevices = deviceRepository.count();

        // Status breakdown
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (SessionStatus status : SessionStatus.values()) {
            statusMap.put(status.name(), 0L);
        }
        List<Object[]> statusCounts = sessionRepository.countSessionsByStatusGroup();
        for (Object[] row : statusCounts) {
            if (row[0] != null && row[1] != null) {
                statusMap.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        // Location breakdown
        List<LocationAnalyticsDto> locationDistribution = new ArrayList<>();
        List<Object[]> locationCounts = sessionRepository.countSessionsByLocation();
        for (Object[] row : locationCounts) {
            if (row[0] != null && row[1] != null) {
                String locName = (String) row[0];
                long count = ((Number) row[1]).longValue();
                double pct = totalSessions > 0 ? ((double) count / totalSessions) * 100.0 : 0.0;
                locationDistribution.add(new LocationAnalyticsDto(locName, count, Math.round(pct * 10.0) / 10.0));
            }
        }

        DashboardOverviewDto dto = new DashboardOverviewDto();
        dto.setTotalSessions(totalSessions);
        dto.setActiveSessions(activeSessions);
        dto.setTotalLocations(totalLocations);
        dto.setActiveLocations(activeLocations);
        dto.setTodaySessions(todaySessions);
        dto.setTotalDevices(totalDevices);
        dto.setGatewayStatus(gatewayAdapter.getStatus());
        dto.setSessionsByStatus(statusMap);
        dto.setLocationDistribution(locationDistribution);

        // Topology metrics calculation - targeted DB counts instead of findAll() memory scan
        long routerOnline = networkDeviceRepository.countByDeviceTypeIgnoreCaseAndStatusIgnoreCase("ROUTER", "ONLINE");
        long routerTotal = networkDeviceRepository.countByDeviceTypeIgnoreCase("ROUTER");
        long routerOffline = Math.max(0, routerTotal - routerOnline);

        long switchOnline = networkDeviceRepository.countByDeviceTypeIgnoreCaseAndStatusIgnoreCase("SWITCH", "ONLINE");
        long switchTotal = networkDeviceRepository.countByDeviceTypeIgnoreCase("SWITCH");
        long switchOffline = Math.max(0, switchTotal - switchOnline);

        // AP theo mô hình "AP theo cổng GWN": kỳ vọng mỗi điểm = min(4, số cổng LAN thật của router).
        // AP Online = tổng cổng LAN ON (active_ap_count); Offline = kỳ vọng - online. Nhất quán với panel theo cổng.
        // Không phụ thuộc AP có đăng ký trong DB hay không (zero-touch).
        final int targetApPerSite = 4;
        java.util.Set<Long> routerLocationIds = networkDeviceRepository.findRouterLocationIds();
        java.util.Map<Long, Integer> apOnByLoc = new java.util.HashMap<>();
        java.util.Map<Long, Integer> apTotalByLoc = new java.util.HashMap<>();
        for (com.wificaptive.locations.entity.WifiLocation loc : locationRepository.findByActiveTrueAndDeletedFalse()) {
            if (loc.getActiveApCount() != null) apOnByLoc.put(loc.getId(), loc.getActiveApCount());
            if (loc.getApTotalCount() != null) apTotalByLoc.put(loc.getId(), loc.getApTotalCount());
        }
        long apOnline = 0, apExpected = 0;
        for (Long locId : routerLocationIds) {
            int on = Math.max(0, apOnByLoc.getOrDefault(locId, 0));
            Integer total = apTotalByLoc.get(locId);
            // kỳ vọng = min(4, số cổng thật); chưa có dữ liệu cổng -> tạm dùng 4.
            int expected = total != null ? Math.min(targetApPerSite, Math.max(0, total)) : targetApPerSite;
            apOnline += on;
            apExpected += expected;
        }
        long apOffline = Math.max(0, apExpected - apOnline); // số AP đang thiếu/off so với kỳ vọng
        long apUnmonitored = 0;

        long clientWireless = activeSessions;
        long clientWired = 0;

        long errorLocs = Math.max(0, totalLocations - activeLocations);
        // "Thiết bị hoạt động" = router ONLINE + AP ONLINE (cổng LAN ON); ghép cặp với "Thiết bị lỗi".
        // KHÔNG tính client (client là cột riêng). Lỗi = router off + AP thiếu/off.
        long totalDevsCount = routerOnline + apOnline;
        long errorDevsCount = routerOffline + apOffline;

        DeviceTopologySummaryDto topology = new DeviceTopologySummaryDto(
                totalLocations,
                errorLocs,
                totalDevsCount,
                errorDevsCount,
                routerOnline,
                routerOffline,
                switchOnline,
                switchOffline,
                apOnline,
                apOffline,
                clientWired,
                clientWireless
        );
        topology.setApUnmonitored(apUnmonitored);
        dto.setTopology(topology);

        this.cachedOverview = new CachedOverview(dto, System.currentTimeMillis());
        if (redisTemplate != null && objectMapper != null) {
            try {
                String json = objectMapper.writeValueAsString(dto);
                redisTemplate.opsForValue().set("cache:analytics:overview", json, 15, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public List<HourlyTrendDto> getHourlyTrends() {
        List<HourlyTrendDto> trends = new ArrayList<>();
        Instant now = Instant.now();

        // Generate past 6 time buckets (4-hour intervals)
        for (int i = 5; i >= 0; i--) {
            Instant bucketStart = now.minus((i + 1) * 4L, ChronoUnit.HOURS);

            String label = String.format("-%dh to -%dh", (i + 1) * 4, i * 4);
            if (i == 0) label = "Last 4h";

            long count = sessionRepository.countSessionsSince(bucketStart);
            trends.add(new HourlyTrendDto(label, Math.max(0, count / (i + 1)), Math.max(0, count / (i + 2))));
        }

        return trends;
    }

    @Transactional(readOnly = true)
    public List<AuthFunnelStepDto> getAuthenticationFunnel() {
        Instant now = Instant.now();

        long step1 = sessionRepository.count();
        long step2 = sessionRepository.countSessionsNotPending();
        long step3 = sessionRepository.countSessionsWithOtp();
        long step4 = sessionRepository.countSessionsAuthenticated();
        long step5 = sessionRepository.countActiveSessions(now);

        long[] counts = {step1, step2, step3, step4, step5};
        String[] names = {
            "Tiếp cận Portal",
            "Đồng ý điều khoản",
            "Nhập SĐT/OTP",
            "Xác thực thành công",
            "Đang hoạt động"
        };

        List<AuthFunnelStepDto> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double retentionPct = step1 > 0
                    ? Math.round((double) counts[i] / step1 * 100.0 * 10.0) / 10.0
                    : 0.0;
            double dropPct = 0.0;
            if (i > 0 && counts[i - 1] > 0) {
                dropPct = Math.round((double) (counts[i - 1] - counts[i]) / counts[i - 1] * 100.0 * 10.0) / 10.0;
            }
            result.add(new AuthFunnelStepDto(i + 1, names[i], counts[i], retentionPct, dropPct));
        }
        return result;
    }
}
