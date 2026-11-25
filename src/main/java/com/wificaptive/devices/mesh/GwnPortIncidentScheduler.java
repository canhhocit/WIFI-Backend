package com.wificaptive.devices.mesh;

import com.wificaptive.common.lock.DistributedLockService;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter;
import com.wificaptive.gateway.dto.GwnRouterPorts;
import com.wificaptive.incidents.dto.CreateIncidentRequest;
import com.wificaptive.incidents.service.IncidentTicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mở phiếu sự cố khi một "AP theo cổng GWN" chuyển OFFLINE (link cổng LAN down), để bảo trì biết
 * ĐÚNG cổng (AP) nào chết. Chỉ xử lý cổng CHƯA gán mesh — cổng đã gán mesh thì MeshGwnStatusScheduler
 * đã mở incident theo thiết bị. Chống trùng: chỉ mở khi cổng chuyển ONLINE->OFFLINE (theo dõi trong bộ nhớ).
 */
@Component
@ConditionalOnProperty(name = "app.mesh-ap.gwn-status-enabled", havingValue = "true", matchIfMissing = true)
public class GwnPortIncidentScheduler {

    private static final Logger log = LoggerFactory.getLogger(GwnPortIncidentScheduler.class);

    private final GwnCloudGatewayAdapter gwnCloud;
    private final NetworkDeviceRepository deviceRepository;
    private final IncidentTicketService incidentService;
    private final DistributedLockService lockService;

    /** Trạng thái up/down lần trước của từng cổng LAN (để phát hiện chuyển OFFLINE). */
    private final Map<String, Boolean> prevUp = new ConcurrentHashMap<>();

    @Autowired
    public GwnPortIncidentScheduler(GwnCloudGatewayAdapter gwnCloud,
                                    NetworkDeviceRepository deviceRepository,
                                    IncidentTicketService incidentService,
                                    DistributedLockService lockService) {
        this.gwnCloud = gwnCloud;
        this.deviceRepository = deviceRepository;
        this.incidentService = incidentService;
        this.lockService = lockService;
    }

    public GwnPortIncidentScheduler(GwnCloudGatewayAdapter gwnCloud,
                                    NetworkDeviceRepository deviceRepository,
                                    IncidentTicketService incidentService) {
        this(gwnCloud, deviceRepository, incidentService, new DistributedLockService(null));
    }

    @Scheduled(fixedDelayString = "${app.mesh-ap.gwn-poll-interval-ms:120000}", initialDelay = 55000)
    public void checkGwnPortIncidents() {
        lockService.runWithLock("scheduler:gwn-port-incident", Duration.ofSeconds(100), this::doCheckGwnPortIncidents);
    }

    @Transactional(readOnly = true)
    public void doCheckGwnPortIncidents() {
        GwnRouterPorts ports = gwnCloud.fetchRouterPorts();
        if (!ports.apiReachable() || !ports.hasLanInfo()) return;

        NetworkDevice gwnRouter = null;
        List<NetworkDevice> routers = deviceRepository.findRouters();
        if (!routers.isEmpty()) {
            gwnRouter = routers.get(0);
        }
        Set<String> boundPorts = new HashSet<>();
        for (NetworkDevice d : deviceRepository.findAccessPoints()) {
            if (MeshApMonitorService.isMeshAp(d) && StringUtils.hasText(d.getGwnLanPort())) {
                boundPorts.add(d.getGwnLanPort().trim());
            }
        }
        Long locationId = gwnRouter != null ? gwnRouter.getLocationId() : null;
        Long gwnDeviceId = gwnRouter != null ? gwnRouter.getId() : null;

        for (Map.Entry<String, Boolean> e : ports.lanUpBySilk().entrySet()) {
            String port = e.getKey();
            boolean up = Boolean.TRUE.equals(e.getValue());
            if (boundPorts.contains(port)) { prevUp.put(port, up); continue; } // mesh scheduler lo cổng đã gán
            Boolean was = prevUp.get(port);
            if (Boolean.TRUE.equals(was) && !up) {
                openPortIncident(locationId, gwnDeviceId, port);
            } else if (Boolean.FALSE.equals(was) && up) {
                closePortIncident(locationId, gwnDeviceId, port);
            }
            prevUp.put(port, up);
        }
    }

    private void openPortIncident(Long locationId, Long gwnDeviceId, String port) {
        try {
            CreateIncidentRequest req = new CreateIncidentRequest();
            req.setLocationId(locationId);
            req.setDeviceId(gwnDeviceId);
            req.setIncidentType("HARDWARE_FAULT");
            req.setSeverity("CRITICAL");
            req.setSlaHours(4);
            req.setTitle("AP cổng LAN" + port + " mất kết nối");
            req.setDescription("Hệ thống phát hiện AP nối cổng LAN" + port + " của router Grandstream GWN "
                    + "đã chuyển OFFLINE (link cổng down). Đề nghị kỹ thuật viên kiểm tra nguồn và cáp của AP ở "
                    + "cổng LAN" + port + ".");
            incidentService.createTicket(req, "system_gwn_port_monitor");
            log.warn("[AUTO INCIDENT] AP cổng LAN{} chuyển OFFLINE -> đã mở phiếu sự cố", port);
        } catch (Exception ex) {
            log.warn("[GwnPortIncident] Không mở được phiếu cho cổng LAN{}: {}", port, ex.getMessage());
        }
    }

    private void closePortIncident(Long locationId, Long gwnDeviceId, String port) {
        try {
            int n = incidentService.resolvePortRecoveredIncidents(gwnDeviceId, locationId, port, null);
            if (n > 0) log.info("[AUTO RESOLVE] Cổng LAN{} UP trở lại -> tự đóng {} phiếu sự cố", port, n);
        } catch (Exception ex) {
            log.warn("[GwnPortIncident] Không đóng được phiếu cho cổng LAN{}: {}", port, ex.getMessage());
        }
    }
}
