package com.wificaptive.analytics.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AnalyticsService {

    private final GuestSessionRepository sessionRepository;
    private final WifiLocationRepository locationRepository;
    private final DeviceRepository deviceRepository;
    private final GatewayAdapter gatewayAdapter;
    private final NetworkDeviceRepository networkDeviceRepository;

    public AnalyticsService(
            GuestSessionRepository sessionRepository,
            WifiLocationRepository locationRepository,
            DeviceRepository deviceRepository,
            GatewayAdapter gatewayAdapter,
            NetworkDeviceRepository networkDeviceRepository) {
        this.sessionRepository = sessionRepository;
        this.locationRepository = locationRepository;
        this.deviceRepository = deviceRepository;
        this.gatewayAdapter = gatewayAdapter;
        this.networkDeviceRepository = networkDeviceRepository;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewDto getOverview() {
        Instant now = Instant.now();
        Instant startOfToday = now.truncatedTo(ChronoUnit.DAYS);

        long totalSessions = sessionRepository.count();
        long activeSessions = sessionRepository.countActiveSessions(now);
        long todaySessions = sessionRepository.countSessionsSince(startOfToday);
        long totalLocations = locationRepository.countByDeletedFalse();
        long activeLocations = locationRepository.countByActiveTrueAndDeletedFalse();
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

        // Topology metrics calculation
        List<NetworkDevice> netDevs = networkDeviceRepository.findAll();
        long routerOnline = netDevs.stream().filter(d -> "ROUTER".equalsIgnoreCase(d.getDeviceType()) && "ONLINE".equalsIgnoreCase(d.getStatus())).count();
        long routerOffline = netDevs.stream().filter(d -> "ROUTER".equalsIgnoreCase(d.getDeviceType()) && !"ONLINE".equalsIgnoreCase(d.getStatus())).count();

        long switchOnline = netDevs.stream().filter(d -> "SWITCH".equalsIgnoreCase(d.getDeviceType()) && "ONLINE".equalsIgnoreCase(d.getStatus())).count();
        long switchOffline = netDevs.stream().filter(d -> "SWITCH".equalsIgnoreCase(d.getDeviceType()) && !"ONLINE".equalsIgnoreCase(d.getStatus())).count();

        long apOnline = netDevs.stream().filter(d -> ("ACCESS_POINT".equalsIgnoreCase(d.getDeviceType()) || "AP".equalsIgnoreCase(d.getDeviceType())) && "ONLINE".equalsIgnoreCase(d.getStatus())).count();
        // UNKNOWN = chưa được giám sát trực tiếp: không tính là Offline để tránh báo lỗi giả
        long apUnmonitored = netDevs.stream().filter(d -> ("ACCESS_POINT".equalsIgnoreCase(d.getDeviceType()) || "AP".equalsIgnoreCase(d.getDeviceType())) && "UNKNOWN".equalsIgnoreCase(d.getStatus())).count();
        long apOffline = netDevs.stream().filter(d -> ("ACCESS_POINT".equalsIgnoreCase(d.getDeviceType()) || "AP".equalsIgnoreCase(d.getDeviceType())) && !"ONLINE".equalsIgnoreCase(d.getStatus()) && !"UNKNOWN".equalsIgnoreCase(d.getStatus())).count();

        long clientWireless = activeSessions;
        long clientWired = 0;

        long errorLocs = Math.max(0, totalLocations - activeLocations);
        long totalDevsCount = netDevs.size();
        long errorDevsCount = netDevs.stream().filter(d -> !"ONLINE".equalsIgnoreCase(d.getStatus()) && !"UNKNOWN".equalsIgnoreCase(d.getStatus())).count();

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
