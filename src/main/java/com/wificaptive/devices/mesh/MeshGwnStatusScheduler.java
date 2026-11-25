package com.wificaptive.devices.mesh;

import com.wificaptive.common.lock.DistributedLockService;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter;
import com.wificaptive.gateway.dto.GwnClientLiveStatus;
import com.wificaptive.gateway.dto.GwnRouterPorts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Suy ra on/off của các mesh AP Viettel từ Grandstream GWN Cloud.
 *
 * Mesh để IP tĩnh (không xin DHCP) nên KHÔNG hiện trong danh sách AP do GWN quản lý, nhưng GWN7002
 * vẫn báo nó lên GDMS cloud như một client NỐI CỔNG LAN (client/info trả wired=1). Nhờ vậy lấy được
 * on/off của mọi mesh qua đúng một kênh GWN Cloud (qua NAT), không cần đăng nhập từng mesh, không cần
 * agent/máy thu tại điểm phát — nên scale nhiều điểm mà không thêm phần cứng.
 *
 * Chỉ áp cho mesh KHÔNG được agent tại chỗ báo cáo (agent cho dữ liệu giàu hơn thì ưu tiên agent).
 */
@Component
@ConditionalOnProperty(name = "app.mesh-ap.gwn-status-enabled", havingValue = "true", matchIfMissing = true)
public class MeshGwnStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(MeshGwnStatusScheduler.class);

    private final GwnCloudGatewayAdapter gwnCloud;
    private final NetworkDeviceRepository deviceRepository;
    private final MeshApMonitorService monitor;
    private final DistributedLockService lockService;
    /** MAC mesh (chuẩn hóa HEX) -> số cổng LAN trên GWN mà mesh đó cắm (silkScreenPort "4","5","6"). */
    private final Map<String, String> portByMac;

    public MeshGwnStatusScheduler(GwnCloudGatewayAdapter gwnCloud,
                                  NetworkDeviceRepository deviceRepository,
                                  MeshApMonitorService monitor,
                                  DistributedLockService lockService,
                                  @Value("${app.mesh-ap.gwn-port-map:}") String gwnPortMapRaw) {
        this.gwnCloud = gwnCloud;
        this.deviceRepository = deviceRepository;
        this.monitor = monitor;
        this.lockService = lockService;
        this.portByMac = parsePortMap(gwnPortMapRaw);
        if (!portByMac.isEmpty()) {
            log.info("[MeshGwnStatus] Khai cổng GWN cho {} mesh: {}", portByMac.size(), portByMac);
        }
    }

    public MeshGwnStatusScheduler(GwnCloudGatewayAdapter gwnCloud,
                                  NetworkDeviceRepository deviceRepository,
                                  MeshApMonitorService monitor,
                                  String gwnPortMapRaw) {
        this(gwnCloud, deviceRepository, monitor, new DistributedLockService(null), gwnPortMapRaw);
    }

    /** Phân tích "MAC=cổng,MAC=cổng" (vd "24:0B:2B:47:2A:22=4,24:0B:2B:46:F8:3A=6"). */
    static Map<String, String> parsePortMap(String raw) {
        Map<String, String> m = new HashMap<>();
        if (!StringUtils.hasText(raw)) return m;
        for (String pair : raw.split(",")) {
            int i = pair.indexOf('=');
            if (i <= 0) continue;
            String mac = pair.substring(0, i).replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
            String silk = pair.substring(i + 1).trim();
            if (mac.length() == 12 && !silk.isEmpty()) m.put(mac, silk);
        }
        return m;
    }

    private static String normMac(String mac) {
        return mac == null ? "" : mac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
    }

    @Scheduled(fixedDelayString = "${app.mesh-ap.gwn-poll-interval-ms:120000}", initialDelay = 40000)
    public void pollMeshStatusViaGwnCloud() {
        lockService.runWithLock("scheduler:mesh-gwn-status", Duration.ofSeconds(100), this::refreshNow);
    }

    /** Chạy ngay một lượt cập nhật on/off từ GWN Cloud (dùng cho nút "làm mới" trên giao diện admin). */
    public int refreshNow() {
        // Trạng thái link cổng LAN của router GWN — tín hiệu NHANH & CHẮC (rút dây là cổng down ngay,
        // không chờ GWN quên client như client/info). Lấy 1 lần dùng cho mọi mesh của cùng router.
        GwnRouterPorts ports = gwnCloud.fetchRouterPorts();
        int online = 0, offline = 0, skipped = 0;
        for (NetworkDevice d : deviceRepository.findAccessPoints()) {
            if (!MeshApMonitorService.isMeshAp(d)) continue;
            if ("DECOMMISSIONED".equalsIgnoreCase(d.getStatus())) continue;
            if (d.getId() != null && monitor.isAgentFed(d.getId())) { skipped++; continue; }
            String mac = d.getMacAddress();
            if (!StringUtils.hasText(mac)) continue;
            try {
                boolean isOnline;
                String reason;
                // Ưu tiên cổng khai trên thiết bị (lúc import); env map chỉ là dự phòng/ghi đè cũ.
                String silk = StringUtils.hasText(d.getGwnLanPort())
                        ? d.getGwnLanPort().trim() : portByMac.get(normMac(mac));
                Boolean portUp = (silk != null && ports.apiReachable()) ? ports.isUp(silk) : null;
                if (portUp != null) {
                    // Chính xác từng mesh: đọc đúng cổng LAN mesh này cắm (hợp cả khi nhiều mesh/1 GWN).
                    isOnline = portUp;
                    reason = "GWN: cổng LAN" + silk + (isOnline ? " up (mesh đang cắm)" : " down (mesh rút/tắt)");
                } else if (silk == null && ports.apiReachable() && ports.hasLanInfo()) {
                    // Chưa khai cổng cho mesh này -> giả định 1 mesh/điểm: còn cổng LAN nào up = mesh còn cắm.
                    isOnline = ports.anyLanUp();
                    reason = isOnline ? "GWN: còn cổng LAN up (chưa khai cổng riêng cho mesh)"
                                      : "GWN: tất cả cổng LAN down (mesh đã rút/tắt)";
                } else {
                    // Dự phòng khi device/info không lấy được: dùng client/info (chậm chiều OFF).
                    GwnClientLiveStatus live = gwnCloud.fetchClientInfoFromCloud(mac);
                    if (!live.apiReachable()) { skipped++; continue; }
                    isOnline = live.exists();
                    reason = isOnline ? "GWN client/info thấy mesh (dự phòng)" : "GWN client/info không thấy mesh (dự phòng)";
                }
                monitor.ingestGwnStatus(d, isOnline, reason);
                if (isOnline) online++; else offline++;
            } catch (Exception e) {
                log.debug("[MeshGwnStatus] Lỗi hỏi GWN Cloud cho {}: {}", mac, e.getMessage());
            }
        }
        if (online + offline > 0) {
            log.info("[MeshGwnStatus] GWN Cloud: {} mesh ONLINE, {} OFFLINE, {} bỏ qua (agent/chưa cấu hình)",
                    online, offline, skipped);
        }
        return online + offline;
    }
}
