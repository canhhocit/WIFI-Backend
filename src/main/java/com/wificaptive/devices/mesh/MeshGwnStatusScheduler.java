package com.wificaptive.devices.mesh;

import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter;
import com.wificaptive.gateway.dto.GwnClientLiveStatus;
import com.wificaptive.gateway.dto.GwnRouterPorts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    public MeshGwnStatusScheduler(GwnCloudGatewayAdapter gwnCloud,
                                  NetworkDeviceRepository deviceRepository,
                                  MeshApMonitorService monitor) {
        this.gwnCloud = gwnCloud;
        this.deviceRepository = deviceRepository;
        this.monitor = monitor;
    }

    @Scheduled(fixedDelayString = "${app.mesh-ap.gwn-poll-interval-ms:120000}", initialDelay = 40000)
    public void pollMeshStatusViaGwnCloud() {
        refreshNow();
    }

    /** Chạy ngay một lượt cập nhật on/off từ GWN Cloud (dùng cho nút "làm mới" trên giao diện admin). */
    public int refreshNow() {
        // Trạng thái link cổng LAN của router GWN — tín hiệu NHANH & CHẮC (rút dây là cổng down ngay,
        // không chờ GWN quên client như client/info). Lấy 1 lần dùng cho mọi mesh của cùng router.
        GwnRouterPorts ports = gwnCloud.fetchRouterPorts();
        int online = 0, offline = 0, skipped = 0;
        for (NetworkDevice d : deviceRepository.findAll()) {
            if (!MeshApMonitorService.isMeshAp(d)) continue;
            if ("DECOMMISSIONED".equalsIgnoreCase(d.getStatus())) continue;
            if (d.getId() != null && monitor.isAgentFed(d.getId())) { skipped++; continue; }
            String mac = d.getMacAddress();
            if (!StringUtils.hasText(mac)) continue;
            try {
                boolean isOnline;
                String reason;
                if (ports.apiReachable() && ports.hasLanInfo()) {
                    // 1 mesh/điểm: còn cổng LAN nào up = mesh còn cắm. Rút dây -> mọi cổng LAN down -> OFFLINE ngay.
                    isOnline = ports.anyLanUp();
                    reason = isOnline ? "GWN: còn cổng LAN up (mesh đang cắm)"
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
