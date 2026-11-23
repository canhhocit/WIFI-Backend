package com.wificaptive.devices.mesh;

import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.incidents.service.IncidentTicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giám sát trực tiếp các thiết bị mesh (Access Point) bằng API ubus của chính thiết bị.
 *
 * Grandstream GWN/GDMS không nhìn thấy mesh vì mesh đặt IP tĩnh (không xin DHCP), nên trạng thái,
 * SSID, client và RSSI của mesh chỉ lấy được bằng cách hỏi thẳng thiết bị. Máy chủ chạy module này
 * phải có đường mạng tới LAN của điểm phát (chạy tại chỗ, hoặc VPN từ gateway về máy chủ).
 *
 * Bật bằng {@code app.mesh-ap.enabled=true}. Khi tắt, AP được báo là "chưa giám sát" — không
 * hiển thị trạng thái giả.
 */
@Service
public class MeshApMonitorService {

    private static final Logger log = LoggerFactory.getLogger(MeshApMonitorService.class);

    public static final String SOURCE_MESH_LIVE = "MESH_LIVE";
    public static final String SOURCE_NOT_MONITORED = "NOT_MONITORED";

    private final MeshApClient client;
    private final NetworkDeviceRepository deviceRepository;
    private final IncidentTicketService incidentTicketService;
    private final TransactionTemplate tx;

    private final boolean enabled;
    private final String username;
    private final String managedPassword;
    private final boolean rotateFactoryPassword;

    private final Map<Long, MeshApSnapshotDto> snapshots = new ConcurrentHashMap<>();

    public MeshApMonitorService(MeshApClient client,
                                NetworkDeviceRepository deviceRepository,
                                IncidentTicketService incidentTicketService,
                                PlatformTransactionManager transactionManager,
                                @Value("${app.mesh-ap.enabled:false}") boolean enabled,
                                @Value("${app.mesh-ap.username:admin}") String username,
                                @Value("${app.mesh-ap.managed-password:}") String managedPassword,
                                @Value("${app.mesh-ap.rotate-factory-password:true}") boolean rotateFactoryPassword) {
        this.client = client;
        this.deviceRepository = deviceRepository;
        this.incidentTicketService = incidentTicketService;
        this.tx = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
        this.username = username;
        this.managedPassword = managedPassword;
        this.rotateFactoryPassword = rotateFactoryPassword;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** AP mesh Viettel HomeWiFi (vAP-xxxx) — loại thiết bị module này đọc được. */
    public static boolean isMeshAp(NetworkDevice d) {
        if (d == null) return false;
        String type = d.getDeviceType() == null ? "" : d.getDeviceType().toUpperCase(Locale.ROOT);
        if (!type.equals("ACCESS_POINT") && !type.equals("AP")) return false;
        String model = d.getModel() == null ? "" : d.getModel().toLowerCase(Locale.ROOT);
        String vendor = d.getVendor() == null ? "" : d.getVendor().toLowerCase(Locale.ROOT);
        return model.startsWith("vap") || vendor.contains("viettel");
    }

    /** Nguồn dữ liệu trạng thái của AP để giao diện hiển thị đúng (không coi số liệu tĩnh là trực tiếp). */
    public String monitoringSource(NetworkDevice d) {
        if (!isMeshAp(d) || !enabled) return SOURCE_NOT_MONITORED;
        MeshApSnapshotDto s = snapshots.get(d.getId());
        if (s == null) return "MESH_PENDING";
        return s.isLive() ? SOURCE_MESH_LIVE : "MESH_" + s.monitoringMode();
    }

    public Optional<MeshApSnapshotDto> cachedSnapshot(Long deviceId) {
        return Optional.ofNullable(snapshots.get(deviceId));
    }

    /**
     * Lấy ảnh chụp trạng thái mesh; {@code refresh=true} đọc lại ngay từ thiết bị.
     */
    public MeshApSnapshotDto getSnapshot(NetworkDevice device, boolean refresh) {
        if (!enabled) {
            return MeshApSnapshotDto.status(device.getId(), device.getIpAddress(), "DISABLED",
                    "Máy chủ này chưa bật giám sát mesh trực tiếp (cần đường mạng VPN/LAN tới điểm phát và app.mesh-ap.enabled=true).",
                    null);
        }
        MeshApSnapshotDto cached = snapshots.get(device.getId());
        if (!refresh && cached != null) return cached;
        return refresh(device);
    }

    /**
     * Đọc thiết bị và cập nhật bản ghi network_devices (trạng thái, firmware, uptime, RAM).
     * Kết nối mạng chạy ngoài transaction; chỉ phần ghi DB nằm trong transaction ngắn.
     */
    public MeshApSnapshotDto refresh(NetworkDevice device) {
        Long id = device.getId();
        String host = device.getIpAddress();
        MeshApSnapshotDto previous = snapshots.get(id);
        Instant lastSuccess = previous != null ? previous.lastSuccessAt() : null;

        if (!StringUtils.hasText(host)) {
            MeshApSnapshotDto s = MeshApSnapshotDto.status(id, null, "NOT_CONFIGURED",
                    "Thiết bị chưa khai báo địa chỉ IP quản trị", lastSuccess);
            snapshots.put(id, s);
            return s;
        }

        String h = host.trim();
        MeshApSnapshotDto snapshot;
        String credentialState = device.getMeshCredentialState();
        boolean rotated = false;
        try {
            LoginResult login = loginWithKnownPasswords(device, h);
            snapshot = login.snapshot();
            credentialState = login.state();
            if (NetworkDevice.CRED_FACTORY.equals(credentialState) && canRotate()) {
                rotated = rotateToManaged(device, h);
                if (rotated) credentialState = NetworkDevice.CRED_MANAGED;
            }
            snapshot = snapshot.withDevice(id, h).withCredentialState(credentialState);
        } catch (MeshApException e) {
            String mode = switch (e.getKind()) {
                case UNREACHABLE -> "UNREACHABLE";
                case AUTH_FAILED -> "AUTH_FAILED";
                default -> "ERROR";
            };
            client.forgetSession(h);
            snapshot = MeshApSnapshotDto.status(id, h, mode, e.getMessage(), lastSuccess);
            log.warn("[MeshAp] {} ({}) -> {}: {}", device.getMacAddress(), h, mode, e.getMessage());
        } catch (RuntimeException e) {
            snapshot = MeshApSnapshotDto.status(id, h, "ERROR", e.getMessage(), lastSuccess);
            log.warn("[MeshAp] {} ({}) lỗi không mong đợi", device.getMacAddress(), h, e);
        }

        final String finalState = credentialState;
        final boolean finalRotated = rotated;
        snapshots.put(id, snapshot);
        final MeshApSnapshotDto result = snapshot;
        tx.executeWithoutResult(status -> applyToDevice(id, result, finalState, finalRotated));
        return snapshot;
    }

    private void applyToDevice(Long id, MeshApSnapshotDto s, String credentialState, boolean rotated) {
        NetworkDevice dev = deviceRepository.findById(id).orElse(null);
        if (dev == null) return;
        String oldStatus = dev.getStatus();
        if (s.isLive() && credentialState != null) dev.setMeshCredentialState(credentialState);
        if (rotated) dev.setMeshPasswordRotatedAt(Instant.now());

        if (s.isLive()) {
            dev.setStatus("ONLINE");
            dev.setLastSeenAt(s.fetchedAt());
            if (StringUtils.hasText(s.firmwareVersion())) dev.setFirmwareVersion(s.firmwareVersion());
            if (StringUtils.hasText(s.model())) dev.setModel(s.model());
            if (s.uptimeSeconds() != null) dev.setUptimeSeconds(s.uptimeSeconds());
            if (s.memoryUsagePercent() != null) dev.setMemoryUsagePercent(s.memoryUsagePercent());
        } else if ("UNREACHABLE".equals(s.monitoringMode())) {
            dev.setStatus("OFFLINE");
        } else if ("AUTH_FAILED".equals(s.monitoringMode()) || "ERROR".equals(s.monitoringMode())) {
            // Thiết bị còn phản hồi nhưng không đọc được dữ liệu
            dev.setStatus("DEGRADED");
        } else {
            return;
        }
        deviceRepository.save(dev);

        if ("OFFLINE".equals(dev.getStatus()) && !"OFFLINE".equalsIgnoreCase(oldStatus)) {
            String locName = dev.getLocation() != null ? dev.getLocation().getName() : "Nhà văn hóa";
            incidentTicketService.recordDeviceFailureIncident(
                    dev.getLocation() != null ? dev.getLocation().getId() : null,
                    dev.getId(),
                    (dev.getModel() != null ? dev.getModel() : "Mesh AP") + " - " + locName,
                    dev.getMacAddress(),
                    "Mesh AP mất kết nối: " + s.message());
        }
    }

    private record LoginResult(MeshApSnapshotDto snapshot, String state) {}

    private boolean canRotate() {
        return rotateFactoryPassword && StringUtils.hasText(managedPassword);
    }

    /**
     * Thử mật khẩu theo thứ tự hợp lý: thiết bị đã đổi -> mật khẩu quản lý trước, S/N sau;
     * thiết bị mới -> S/N trước, mật khẩu quản lý sau (phòng trường hợp đã được đổi tay khi lắp đặt).
     */
    private LoginResult loginWithKnownPasswords(NetworkDevice device, String host) throws MeshApException {
        String serial = device.getSerialNumber();
        boolean hasManaged = StringUtils.hasText(managedPassword);
        boolean managedFirst = NetworkDevice.CRED_MANAGED.equals(device.getMeshCredentialState()) && hasManaged;

        java.util.List<String[]> attempts = new java.util.ArrayList<>();
        if (managedFirst) attempts.add(new String[]{managedPassword, NetworkDevice.CRED_MANAGED});
        if (StringUtils.hasText(serial)) attempts.add(new String[]{serial, NetworkDevice.CRED_FACTORY});
        if (!managedFirst && hasManaged) attempts.add(new String[]{managedPassword, NetworkDevice.CRED_MANAGED});
        if (attempts.isEmpty()) {
            throw new MeshApException(MeshApException.Kind.AUTH_FAILED, "Thiết bị chưa có S/N để đăng nhập lần đầu");
        }

        MeshApException last = null;
        for (String[] a : attempts) {
            try {
                MeshApSnapshotDto snap = client.fetchSnapshot(host, username, a[0]);
                return new LoginResult(snap, a[1]);
            } catch (MeshApException e) {
                if (e.getKind() != MeshApException.Kind.AUTH_FAILED) throw e;
                client.forgetSession(host);
                last = e;
            }
        }
        throw new MeshApException(MeshApException.Kind.AUTH_FAILED,
                "Thiết bị từ chối cả S/N lẫn mật khẩu quản lý chung — kiểm tra S/N đã import hoặc mật khẩu đã bị đổi tay",
                last);
    }

    /** Đổi mật khẩu gốc (S/N in trên nhãn) sang mật khẩu quản lý chung, rồi đăng nhập lại để xác nhận. */
    private boolean rotateToManaged(NetworkDevice device, String host) {
        try {
            client.changePassword(host, username, managedPassword);
            client.fetchSnapshot(host, username, managedPassword);
            log.info("[MeshAp] {} ({}) đã đổi mật khẩu gốc sang mật khẩu quản lý chung", device.getMacAddress(), host);
            return true;
        } catch (MeshApException e) {
            client.forgetSession(host);
            log.error("[MeshAp] {} ({}) đổi mật khẩu quản lý thất bại: {}", device.getMacAddress(), host, e.getMessage());
            return false;
        }
    }

    /** Quét định kỳ toàn bộ mesh AP (mặc định mỗi 60 giây). */
    @Scheduled(fixedDelayString = "${app.mesh-ap.poll-interval-ms:60000}", initialDelay = 20000)
    public void pollAll() {
        if (!enabled) return;
        List<NetworkDevice> aps = deviceRepository.findAll().stream()
                .filter(MeshApMonitorService::isMeshAp)
                .filter(d -> !"DECOMMISSIONED".equalsIgnoreCase(d.getStatus()))
                .toList();
        int live = 0;
        for (NetworkDevice ap : aps) {
            if (refresh(ap).isLive()) live++;
        }
        log.info("[MeshAp] Quét {} mesh AP: {} đọc được, {} lỗi/mất kết nối", aps.size(), live, aps.size() - live);
    }
}
