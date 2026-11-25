package com.wificaptive.devices.mesh;

import com.wificaptive.common.lock.DistributedLockService;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter;
import com.wificaptive.gateway.dto.GwnRouterPorts;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Cache "số AP thực đang kết nối" của từng điểm = số cổng LAN ON trên router GWN của điểm.
 *
 * Mô hình: mỗi điểm 1 router GWN + {expected} AP (mặc định 4). AP là zero-touch (không đăng ký),
 * nên không biết được từ DB — phải đọc cổng LAN của router qua GWN Cloud. Để danh sách 1.584 điểm
 * không gọi GWN mỗi lần mở trang, scheduler này chạy nền đọc cổng theo chu kỳ rồi ghi số cổng ON vào
 * {@code wifi_locations.active_ap_count}; danh sách và màu sức khỏe đọc từ DB (nhanh).
 */
@Component
@ConditionalOnProperty(name = "app.mesh-ap.gwn-status-enabled", havingValue = "true", matchIfMissing = true)
public class LocationApCountScheduler {

    private static final Logger log = LoggerFactory.getLogger(LocationApCountScheduler.class);

    private final GwnCloudGatewayAdapter gwnCloud;
    private final NetworkDeviceRepository deviceRepository;
    private final WifiLocationRepository locationRepository;
    private final DistributedLockService lockService;

    public LocationApCountScheduler(GwnCloudGatewayAdapter gwnCloud,
                                    NetworkDeviceRepository deviceRepository,
                                    WifiLocationRepository locationRepository,
                                    DistributedLockService lockService) {
        this.gwnCloud = gwnCloud;
        this.deviceRepository = deviceRepository;
        this.locationRepository = locationRepository;
        this.lockService = lockService;
    }

    public LocationApCountScheduler(GwnCloudGatewayAdapter gwnCloud,
                                    NetworkDeviceRepository deviceRepository,
                                    WifiLocationRepository locationRepository) {
        this(gwnCloud, deviceRepository, locationRepository, new DistributedLockService(null));
    }

    @Scheduled(fixedDelayString = "${app.mesh-ap.ap-count-interval-ms:300000}", initialDelay = 70000)
    public void refreshLocationApCounts() {
        lockService.runWithLock("scheduler:location-ap-count", Duration.ofMinutes(4), this::doRefreshLocationApCounts);
    }

    @Transactional
    public void doRefreshLocationApCounts() {
        int updated = 0, unreachable = 0;
        for (NetworkDevice d : deviceRepository.findRouters()) {
            if (d.getLocationId() == null || !StringUtils.hasText(d.getMacAddress())) continue;
            try {
                String mac = d.getMacAddress().replaceAll("[^0-9A-Fa-f]", "");
                GwnRouterPorts ports = gwnCloud.fetchRouterPorts(mac, d.getGwnNetworkId());
                if (!ports.apiReachable()) { unreachable++; continue; } // không ghi đè khi không đọc được
                int on = 0;
                int total = ports.lanUpBySilk().size(); // tổng cổng LAN RJ45 (đã loại SFP ở parseRouterPorts)
                for (Boolean up : ports.lanUpBySilk().values()) {
                    if (Boolean.TRUE.equals(up)) on++;
                }
                locationRepository.updateApCounts(d.getLocationId(), on, total);
                updated++;
            } catch (Exception e) {
                log.debug("[LocationApCount] Lỗi đọc cổng router {} (điểm {}): {}", d.getMacAddress(), d.getLocationId(), e.getMessage());
            }
        }
        if (updated > 0 || unreachable > 0) {
            log.info("[LocationApCount] Cập nhật số AP/điểm: {} điểm, {} router không đọc được cổng", updated, unreachable);
        }
    }
}
