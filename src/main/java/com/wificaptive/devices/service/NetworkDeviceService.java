package com.wificaptive.devices.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.devices.dto.DeviceSwapRequest;
import com.wificaptive.devices.dto.DeviceSyncSummaryDto;
import com.wificaptive.devices.dto.NetworkDeviceDto;
import com.wificaptive.devices.entity.DeviceSwapLog;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.mesh.MeshApMonitorService;
import com.wificaptive.devices.mesh.MeshApSnapshotDto;
import com.wificaptive.devices.repository.DeviceSwapLogRepository;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter;
import com.wificaptive.gateway.dto.GwnDeviceSyncDto;
import com.wificaptive.gateway.dto.GwnRebootResult;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class NetworkDeviceService {

    private static final Logger log = LoggerFactory.getLogger(NetworkDeviceService.class);

    private final NetworkDeviceRepository deviceRepository;
    private final DeviceSwapLogRepository swapLogRepository;
    private final WifiLocationRepository locationRepository;
    private final AuditService auditService;
    private final GwnCloudGatewayAdapter gwnCloudGatewayAdapter;
    private final com.wificaptive.incidents.service.IncidentTicketService incidentTicketService;
    private final MeshApMonitorService meshApMonitorService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wificaptive.geography.service.AdminScopeService adminScopeService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wificaptive.sessions.repository.GuestSessionRepository guestSessionRepository;

    public NetworkDeviceService(NetworkDeviceRepository deviceRepository,
                                DeviceSwapLogRepository swapLogRepository,
                                WifiLocationRepository locationRepository,
                                AuditService auditService,
                                GwnCloudGatewayAdapter gwnCloudGatewayAdapter,
                                com.wificaptive.incidents.service.IncidentTicketService incidentTicketService,
                                MeshApMonitorService meshApMonitorService) {
        this.deviceRepository = deviceRepository;
        this.swapLogRepository = swapLogRepository;
        this.locationRepository = locationRepository;
        this.auditService = auditService;
        this.gwnCloudGatewayAdapter = gwnCloudGatewayAdapter;
        this.incidentTicketService = incidentTicketService;
        this.meshApMonitorService = meshApMonitorService;
    }

    public void setAdminScopeService(com.wificaptive.geography.service.AdminScopeService adminScopeService) {
        this.adminScopeService = adminScopeService;
    }

    @Transactional(readOnly = true)
    public PageResponse<NetworkDeviceDto> getDevices(String type, String status, Long adminUnitId, Pageable pageable) {
        java.util.Set<Long> effectiveUnitIds = null;
        java.util.Set<Long> userScopedIds = adminScopeService != null ? adminScopeService.getCurrentUserScopedUnitIds() : null;

        if (adminUnitId != null) {
            java.util.Set<Long> requestedIds = adminScopeService != null
                    ? adminScopeService.expandUnitAndDescendantIds(adminUnitId)
                    : java.util.Set.of(adminUnitId);
            if (userScopedIds != null) {
                effectiveUnitIds = requestedIds.stream()
                        .filter(userScopedIds::contains)
                        .collect(Collectors.toSet());
            } else {
                effectiveUnitIds = requestedIds;
            }
        } else if (userScopedIds != null) {
            effectiveUnitIds = userScopedIds;
        }

        String normType = null;
        if (type != null && !type.isBlank()) {
            normType = type.trim().toUpperCase();
            if ("AP".equalsIgnoreCase(normType)) {
                normType = "ACCESS_POINT";
            }
        }
        String normStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        Page<NetworkDevice> page;
        if (effectiveUnitIds != null) {
            if (effectiveUnitIds.isEmpty()) {
                page = Page.empty(pageable);
            } else if (normType != null) {
                page = deviceRepository.findByDeviceTypeAndLocationAdminUnitIdIn(normType, effectiveUnitIds, pageable);
            } else if (normStatus != null) {
                page = deviceRepository.findByStatusAndLocationAdminUnitIdIn(normStatus, effectiveUnitIds, pageable);
            } else {
                page = deviceRepository.findByLocationAdminUnitIdIn(effectiveUnitIds, pageable);
            }
        } else {
            if (normType != null) {
                page = deviceRepository.findByDeviceType(normType, pageable);
            } else if (normStatus != null) {
                page = deviceRepository.findByStatus(normStatus, pageable);
            } else {
                page = deviceRepository.findAll(pageable);
            }
        }
        return PageResponse.from(page.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public List<NetworkDeviceDto> getDevicesByLocation(Long locationId) {
        return deviceRepository.findByLocationId(locationId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NetworkDeviceDto> getUnassignedDevices() {
        return deviceRepository.findByLocationIsNullOrderByUpdatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NetworkDeviceDto getDeviceById(Long id) {
        NetworkDevice dev = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));
        return toDto(dev);
    }

    public NetworkDeviceDto createDevice(com.wificaptive.devices.dto.NetworkDeviceUpsertRequest req, String actorUsername) {
        String normMac = normalizeMacAddress(req.getMacAddress());
        if (normMac == null) {
            throw new BadRequestException("Địa chỉ MAC không hợp lệ: " + req.getMacAddress());
        }
        String cleanSerial = req.getSerialNumber().trim();
        if (deviceRepository.existsByMacAddress(normMac)) {
            throw new BadRequestException("Địa chỉ MAC đã tồn tại trên hệ thống: " + normMac);
        }
        if (deviceRepository.existsBySerialNumber(cleanSerial)) {
            throw new BadRequestException("Số Serial (S/N) đã tồn tại trên hệ thống: " + cleanSerial);
        }

        NetworkDevice dev = new NetworkDevice();
        dev.setMacAddress(normMac);
        dev.setSerialNumber(cleanSerial);
        dev.setDeviceType(normalizeDeviceType(req.getDeviceType()));
        dev.setName(trimToNull(req.getName()));
        dev.setModel(trimToNull(req.getModel()));
        dev.setVendor(trimToNull(req.getVendor()));
        dev.setIpAddress(trimToNull(req.getIpAddress()));
        dev.setGwnNetworkId(trimToNull(req.getGwnNetworkId()));
        dev.setStatus(req.getStatus() != null && !req.getStatus().isBlank() ? req.getStatus().trim().toUpperCase() : "UNKNOWN");
        dev.setInstalledAt(Instant.now());

        if (req.getLocationId() != null) {
            WifiLocation loc = locationRepository.findById(req.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("WifiLocation", "id", req.getLocationId()));
            dev.setLocation(loc);
        }

        NetworkDevice saved = deviceRepository.save(dev);
        auditService.recordEvent(
                AuditAction.DEVICE_SYNC,
                actorUsername != null ? actorUsername : "admin",
                "internal",
                "NETWORK_DEVICE",
                saved.getId().toString(),
                "Thêm mới thiết bị " + saved.getDeviceType() + " (" + saved.getMacAddress() + ")"
                        + (saved.getLocation() != null ? " tại điểm " + saved.getLocation().getName() : " (chưa gán điểm)")
        );
        return toDto(saved);
    }

    public NetworkDeviceDto updateDevice(Long id, com.wificaptive.devices.dto.NetworkDeviceUpsertRequest req, String actorUsername) {
        NetworkDevice dev = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));

        String normMac = normalizeMacAddress(req.getMacAddress());
        if (normMac == null) {
            throw new BadRequestException("Địa chỉ MAC không hợp lệ: " + req.getMacAddress());
        }
        String cleanSerial = req.getSerialNumber().trim();

        deviceRepository.findByMacAddress(normMac).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new BadRequestException("Địa chỉ MAC đã thuộc thiết bị khác: " + normMac);
            }
        });
        deviceRepository.findBySerialNumber(cleanSerial).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new BadRequestException("Số Serial (S/N) đã thuộc thiết bị khác: " + cleanSerial);
            }
        });

        dev.setMacAddress(normMac);
        dev.setSerialNumber(cleanSerial);
        dev.setDeviceType(normalizeDeviceType(req.getDeviceType()));
        dev.setName(trimToNull(req.getName()));
        dev.setModel(trimToNull(req.getModel()));
        dev.setVendor(trimToNull(req.getVendor()));
        dev.setIpAddress(trimToNull(req.getIpAddress()));
        dev.setGwnNetworkId(trimToNull(req.getGwnNetworkId()));
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            dev.setStatus(req.getStatus().trim().toUpperCase());
        }

        if (req.getLocationId() != null) {
            WifiLocation loc = locationRepository.findById(req.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("WifiLocation", "id", req.getLocationId()));
            dev.setLocation(loc);
        } else {
            dev.setLocation(null);
        }

        NetworkDevice saved = deviceRepository.save(dev);
        auditService.recordEvent(
                AuditAction.LOCATION_UPDATE,
                actorUsername != null ? actorUsername : "admin",
                "internal",
                "NETWORK_DEVICE",
                saved.getId().toString(),
                "Cập nhật thiết bị " + saved.getMacAddress()
                        + (saved.getLocation() != null ? " -> điểm " + saved.getLocation().getName() : " (gỡ khỏi điểm)")
        );
        return toDto(saved);
    }

    public NetworkDeviceDto assignDeviceToLocation(Long id, com.wificaptive.devices.dto.AssignDeviceLocationRequest req, String actorUsername) {
        NetworkDevice dev = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));

        if (req != null && req.getLocationId() != null) {
            WifiLocation loc = locationRepository.findById(req.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("WifiLocation", "id", req.getLocationId()));
            dev.setLocation(loc);
        } else {
            dev.setLocation(null);
        }

        if (req != null && req.getName() != null) {
            dev.setName(trimToNull(req.getName()));
        }
        if (req != null && req.getGwnNetworkId() != null) {
            dev.setGwnNetworkId(trimToNull(req.getGwnNetworkId()));
        }

        NetworkDevice saved = deviceRepository.save(dev);
        auditService.recordEvent(
                AuditAction.LOCATION_UPDATE,
                actorUsername != null ? actorUsername : "admin",
                "internal",
                "NETWORK_DEVICE",
                saved.getId().toString(),
                saved.getLocation() != null
                        ? "Gán thiết bị " + saved.getMacAddress() + " vào điểm " + saved.getLocation().getName()
                        : "Gỡ thiết bị " + saved.getMacAddress() + " khỏi điểm phát"
        );
        return toDto(saved);
    }

    private String normalizeMacAddress(String raw) {
        if (raw == null) return null;
        String hex = raw.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        if (hex.length() != 12) return null;
        return String.format("%s:%s:%s:%s:%s:%s",
                hex.substring(0, 2), hex.substring(2, 4), hex.substring(4, 6),
                hex.substring(6, 8), hex.substring(8, 10), hex.substring(10, 12));
    }

    private String normalizeDeviceType(String raw) {
        if (raw == null || raw.isBlank()) return "ACCESS_POINT";
        String upper = raw.trim().toUpperCase();
        if ("AP".equals(upper)) return "ACCESS_POINT";
        return upper;
    }

    private String trimToNull(String val) {
        return (val != null && !val.isBlank()) ? val.trim() : null;
    }

    /** DTO kèm nguồn dữ liệu giám sát, để giao diện không hiển thị số liệu tĩnh như dữ liệu trực tiếp. */
    private NetworkDeviceDto toDto(NetworkDevice dev) {
        NetworkDeviceDto dto = NetworkDeviceDto.from(dev);
        if (MeshApMonitorService.isMeshAp(dev)) {
            dto.setMonitoringSource(meshApMonitorService.monitoringSource(dev));
        } else if ("ROUTER".equalsIgnoreCase(dev.getDeviceType())) {
            dto.setMonitoringSource(gwnCloudGatewayAdapter.isLiveCredentialsConfigured() ? "GWN_CLOUD" : "GWN_SIMULATED");
        } else {
            dto.setMonitoringSource(MeshApMonitorService.SOURCE_NOT_MONITORED);
        }
        return dto;
    }

    /** Trạng thái mesh AP đọc trực tiếp từ thiết bị (SSID, kênh, client, RSSI, uptime, RAM). */
    public MeshApSnapshotDto getMeshSnapshot(Long id, boolean refresh) {
        NetworkDevice dev = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));
        if (!MeshApMonitorService.isMeshAp(dev)) {
            throw new BadRequestException("Thiết bị không phải mesh AP Viettel HomeWiFi: " + dev.getModel());
        }
        return meshApMonitorService.getSnapshot(dev, refresh);
    }

    /**
     * "AP theo cổng GWN": mỗi cổng LAN của router GWN đang cắm (hoặc đã gán mesh) = 1 AP của điểm.
     * On/off đọc từ linkStatus cổng (device/info) — bảo trì biết ĐÚNG cổng (AP) nào chết, không cần MAC.
     * Cổng nào đã gán mesh (import có cổng) thì kèm tên/MAC/IP; chưa gán thì chỉ hiện theo cổng.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getGwnPortStatus() {
        return getGwnPortStatus(null);
    }

    /**
     * Trạng thái "AP theo cổng GWN" của ĐÚNG một địa điểm: tìm router GWN gắn với điểm rồi đọc cổng LAN
     * của chính router đó (theo MAC + gwnNetworkId per-device). Truyền null -> router đầu tiên (tương
     * thích bản 1-GWN cũ). Nhờ vậy giao diện xem theo từng điểm, scale 1500 điểm không dồn hết 1 bảng.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getGwnPortStatus(Long locationId) {
        java.util.List<NetworkDevice> scope;
        if (locationId != null) {
            scope = deviceRepository.findByLocationId(locationId);
        } else {
            java.util.List<NetworkDevice> sampleRouters = deviceRepository.findRouters(org.springframework.data.domain.PageRequest.of(0, 1)).getContent();
            if (!sampleRouters.isEmpty() && sampleRouters.get(0).getLocationId() != null) {
                scope = deviceRepository.findByLocationId(sampleRouters.get(0).getLocationId());
            } else {
                scope = sampleRouters;
            }
        }
        Map<String, NetworkDevice> meshByPort = new java.util.HashMap<>();
        NetworkDevice gwnRouter = null;
        for (NetworkDevice d : scope) {
            if ("ROUTER".equalsIgnoreCase(d.getDeviceType()) && gwnRouter == null) gwnRouter = d;
            if (MeshApMonitorService.isMeshAp(d) && org.springframework.util.StringUtils.hasText(d.getGwnLanPort())) {
                meshByPort.put(d.getGwnLanPort().trim(), d);
            }
        }
        // Đọc cổng của đúng router điểm này (MAC dạng hex không dấu để khớp device/info); fallback .env.
        com.wificaptive.gateway.dto.GwnRouterPorts ports = gwnRouter != null
                ? gwnCloudGatewayAdapter.fetchRouterPorts(
                        gwnRouter.getMacAddress() != null ? gwnRouter.getMacAddress().replaceAll("[^0-9A-Fa-f]", "") : null,
                        gwnRouter.getGwnNetworkId())
                : gwnCloudGatewayAdapter.fetchRouterPorts();
        // Kỳ vọng = min(4, số cổng LAN thật). Luôn hiện cổng ON + cổng đã gán mesh; bổ sung cổng TRỐNG
        // đang OFF cho đủ "kỳ vọng" slot (để lộ AP còn thiếu), và ẨN cổng trống dư trên router nhiều cổng.
        int onCount = 0;
        for (Boolean v : ports.lanUpBySilk().values()) if (Boolean.TRUE.equals(v)) onCount++;
        final int effectiveExpected = Math.min(4, ports.lanUpBySilk().size());

        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        java.util.List<Map.Entry<String, Boolean>> unboundOff = new java.util.ArrayList<>();
        for (Map.Entry<String, Boolean> e : ports.lanUpBySilk().entrySet()) {
            boolean up = Boolean.TRUE.equals(e.getValue());
            NetworkDevice mesh = meshByPort.get(e.getKey());
            if (!up && mesh == null) { unboundOff.add(e); continue; } // để lọc sau (chỉ hiện đủ số slot thiếu)
            list.add(portCard(e.getKey(), up, mesh));
        }
        // Bổ sung cổng trống OFF cho đủ "kỳ vọng" slot (phần còn thiếu), ẩn phần dư.
        int slotsLeft = effectiveExpected - list.size();
        for (Map.Entry<String, Boolean> e : unboundOff) {
            if (slotsLeft <= 0) break;
            list.add(portCard(e.getKey(), false, null));
            slotsLeft--;
        }
        list.sort(java.util.Comparator.comparing(m -> (String) m.get("port")));
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("apiReachable", ports.apiReachable());
        out.put("gwnName", gwnRouter != null ? gwnRouter.getName() : null);
        out.put("siteName", gwnRouter != null && gwnRouter.getLocation() != null ? gwnRouter.getLocation().getName() : null);
        out.put("ports", list);
        return out;
    }

    /** Một thẻ cổng AP cho panel "AP theo cổng GWN". */
    private Map<String, Object> portCard(String silk, boolean up, NetworkDevice mesh) {
        Map<String, Object> pm = new java.util.LinkedHashMap<>();
        pm.put("port", silk);
        pm.put("label", "AP cổng LAN" + silk);
        pm.put("status", up ? "ONLINE" : "OFFLINE");
        pm.put("meshName", mesh != null
                ? (org.springframework.util.StringUtils.hasText(mesh.getName()) ? mesh.getName() : mesh.getMacAddress())
                : null);
        pm.put("meshMac", mesh != null ? mesh.getMacAddress() : null);
        pm.put("meshIp", mesh != null ? mesh.getIpAddress() : null);
        return pm;
    }

    public NetworkDeviceDto swapDevice(DeviceSwapRequest request, String actorUsername) {
        WifiLocation location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("WifiLocation", "locationId", request.getLocationId()));

        String cleanNewMac = request.getNewMacAddress().trim().toUpperCase();
        if (deviceRepository.existsByMacAddress(cleanNewMac)) {
            throw new BadRequestException("Địa chỉ MAC mới đã tồn tại trên hệ thống: " + cleanNewMac);
        }
        if (deviceRepository.existsBySerialNumber(request.getNewSerialNumber().trim())) {
            throw new BadRequestException("Số Serial mới đã tồn tại: " + request.getNewSerialNumber());
        }

        NetworkDevice oldDevice = null;
        if (request.getOldDeviceId() != null) {
            oldDevice = deviceRepository.findById(request.getOldDeviceId()).orElse(null);
        }

        // Decommission old device
        if (oldDevice != null) {
            oldDevice.setStatus("DECOMMISSIONED");
            deviceRepository.save(oldDevice);
        }

        // Create new device
        NetworkDevice newDev = new NetworkDevice();
        newDev.setLocation(location);
        newDev.setDeviceType(request.getDeviceType().toUpperCase());
        newDev.setSerialNumber(request.getNewSerialNumber().trim());
        newDev.setMacAddress(cleanNewMac);
        newDev.setModel(request.getNewModel() != null ? request.getNewModel() : (oldDevice != null ? oldDevice.getModel() : "Default"));
        newDev.setVendor(request.getNewVendor() != null ? request.getNewVendor() : (oldDevice != null ? oldDevice.getVendor() : "Default"));
        // Thay mesh cùng ổ LAN: kế thừa CỔNG GWN (và IP tĩnh) của máy cũ -> giám sát on/off liền mạch,
        // "AP cổng LANx" tự chuyển sang mesh mới mà không phải cấu hình lại.
        if (oldDevice != null) {
            newDev.setGwnLanPort(oldDevice.getGwnLanPort());
            if (!org.springframework.util.StringUtils.hasText(newDev.getIpAddress())) {
                newDev.setIpAddress(oldDevice.getIpAddress());
            }
        }
        newDev.setStatus("ONLINE");
        newDev.setInstalledAt(Instant.now());
        newDev.setLastSeenAt(Instant.now());
        NetworkDevice savedNewDev = deviceRepository.save(newDev);

        // Record swap log
        DeviceSwapLog log = new DeviceSwapLog();
        log.setLocation(location);
        log.setOldDevice(oldDevice);
        log.setNewDevice(savedNewDev);
        if (oldDevice != null) {
            log.setOldSerialNumber(oldDevice.getSerialNumber());
            log.setOldMacAddress(oldDevice.getMacAddress());
        }
        log.setNewSerialNumber(savedNewDev.getSerialNumber());
        log.setNewMacAddress(savedNewDev.getMacAddress());
        log.setSwapReason(request.getSwapReason());
        log.setSwappedBy(actorUsername != null ? actorUsername : "system");
        log.setSwappedAt(Instant.now());
        swapLogRepository.save(log);

        auditService.recordEvent(
                AuditAction.LOCATION_UPDATE,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "DEVICE_SWAP",
                savedNewDev.getId().toString(),
                "Hoán đổi thiết bị " + request.getDeviceType() + " tại " + location.getName() + ": MAC cũ=" +
                        (oldDevice != null ? oldDevice.getMacAddress() : "None") + " -> MAC mới=" + savedNewDev.getMacAddress()
        );

        return NetworkDeviceDto.from(savedNewDev);
    }

    private String resolveGrandstreamDeviceType(String model) {
        if (model == null || model.isBlank()) {
            return "ACCESS_POINT";
        }
        String upper = model.trim().toUpperCase();
        if (upper.startsWith("GWN70") || upper.contains("ROUTER") || upper.contains("GATEWAY")) {
            return "ROUTER";
        }
        return "ACCESS_POINT";
    }

    public DeviceSyncSummaryDto syncFromGrandstream(String actorUsername) {
        List<GwnDeviceSyncDto> cloudDevices = gwnCloudGatewayAdapter.fetchDevicesFromCloud();
        int updatedCount = 0;
        int createdCount = 0;
        List<NetworkDeviceDto> resultList = new ArrayList<>();

        for (GwnDeviceSyncDto cDev : cloudDevices) {
            String normMac = cDev.getMac().toUpperCase();
            String cleanMac = normMac.replaceAll("[^A-F0-9]", "");

            NetworkDevice dev = deviceRepository.findByMacAddress(normMac)
                    .or(() -> deviceRepository.findByCleanMac(cleanMac.toLowerCase()))
                    .orElse(null);

            if (dev != null) {
                dev.setStatus(cDev.getStatus());
                if (cDev.getIp() != null && !cDev.getIp().isBlank()) {
                    dev.setIpAddress(cDev.getIp());
                }
                if (cDev.getFirmware() != null && !cDev.getFirmware().isBlank()) {
                    dev.setFirmwareVersion(cDev.getFirmware());
                }
                if (cDev.getCpuPercent() != null) {
                    dev.setCpuUsagePercent(cDev.getCpuPercent());
                }
                if (cDev.getMemoryPercent() != null) {
                    dev.setMemoryUsagePercent(cDev.getMemoryPercent());
                }
                if (cDev.getUptimeSeconds() != null) {
                    dev.setUptimeSeconds(cDev.getUptimeSeconds());
                }
                if (dev.getVendor() == null || dev.getVendor().isBlank()) {
                    dev.setVendor("Grandstream");
                }
                if ((dev.getModel() == null || dev.getModel().isBlank()) && cDev.getModel() != null && !cDev.getModel().isBlank()) {
                    dev.setModel(cDev.getModel());
                }
                dev.setLastSeenAt(Instant.now());
                NetworkDevice saved = deviceRepository.save(dev);
                resultList.add(NetworkDeviceDto.from(saved));
                updatedCount++;

                // Tự động kích hoạt tạo Phiếu sự cố khi thiết bị báo OFFLINE
                if ("OFFLINE".equalsIgnoreCase(cDev.getStatus())) {
                    String locName = dev.getLocation() != null ? dev.getLocation().getName() : "Chưa gán điểm";
                    incidentTicketService.recordDeviceFailureIncident(
                            dev.getLocation() != null ? dev.getLocation().getId() : null,
                            dev.getId(),
                            (dev.getModel() != null ? dev.getModel() : dev.getDeviceType()) + " - " + locName,
                            dev.getMacAddress(),
                            "Thiết bị báo mất kết nối (OFFLINE) trong phiên quét định kỳ từ Grandstream GDMS/GWN Cloud"
                    );
                }
            } else {
                NetworkDevice newDev = new NetworkDevice();
                newDev.setLocation(null); // Không gán bừa vào Nhà văn hóa mặc định; admin tự gán điểm hoặc giữ trạng thái chưa gán
                newDev.setDeviceType(resolveGrandstreamDeviceType(cDev.getModel()));
                newDev.setVendor("Grandstream");
                newDev.setModel(cDev.getModel() != null && !cDev.getModel().isBlank() ? cDev.getModel() : "GWN");
                newDev.setSerialNumber(cleanMac);
                newDev.setMacAddress(normMac);
                newDev.setIpAddress(cDev.getIp());
                newDev.setStatus(cDev.getStatus());
                newDev.setFirmwareVersion(cDev.getFirmware());
                newDev.setCpuUsagePercent(cDev.getCpuPercent() != null ? cDev.getCpuPercent() : 0);
                newDev.setMemoryUsagePercent(cDev.getMemoryPercent() != null ? cDev.getMemoryPercent() : 0);
                newDev.setUptimeSeconds(cDev.getUptimeSeconds() != null ? cDev.getUptimeSeconds() : 0L);
                newDev.setInstalledAt(Instant.now());
                newDev.setLastSeenAt(Instant.now());
                NetworkDevice saved = deviceRepository.save(newDev);
                resultList.add(NetworkDeviceDto.from(saved));
                createdCount++;
            }
        }

        auditService.recordEvent(
                AuditAction.DEVICE_SYNC,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "NETWORK_DEVICE",
                "ALL",
                "Đồng bộ Grandstream Cloud: nhận " + cloudDevices.size() + " thiết bị (Cập nhật: " + updatedCount + ", Thêm mới: " + createdCount + ")"
        );

        String msg = cloudDevices.isEmpty()
                ? "Grandstream Cloud trả về 0 thiết bị (chưa có thiết bị nào từ API Cloud)."
                : String.format("Đồng bộ hoàn tất: %d thiết bị được cập nhật trạng thái thực tế từ Grandstream Cloud.", updatedCount + createdCount);
        return new DeviceSyncSummaryDto(cloudDevices.size(), updatedCount, createdCount, resultList, msg, !gwnCloudGatewayAdapter.isLiveCredentialsConfigured());
    }

    public NetworkDeviceDto rebootDevice(Long id, String actorUsername) {
        NetworkDevice dev = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));

        GwnRebootResult result = (dev.getGwnNetworkId() != null && !dev.getGwnNetworkId().isBlank())
                ? gwnCloudGatewayAdapter.rebootDeviceOnCloud(dev.getMacAddress(), dev.getGwnNetworkId())
                : gwnCloudGatewayAdapter.rebootDeviceOnCloud(dev.getMacAddress());
        if (!result.isSuccess()) {
            throw new BadRequestException("Không thể gửi lệnh khởi động lại: " + result.getMessage());
        }

        dev.setStatus("REBOOTING");
        dev.setLastSeenAt(Instant.now());
        NetworkDevice saved = deviceRepository.save(dev);

        auditService.recordEvent(
                AuditAction.DEVICE_REBOOT,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "NETWORK_DEVICE",
                saved.getId().toString(),
                "Phát lệnh khởi động lại phần cứng cho Router " + saved.getModel() + " (MAC: " + saved.getMacAddress() + ") qua Grandstream Cloud. " + result.getMessage()
        );

        return NetworkDeviceDto.from(saved);
    }

    /** Client đang kết nối (từ GDMS) của router GWN tại điểm — để liệt kê + thống kê theo cổng/AP. */
    public List<com.wificaptive.gateway.dto.GwnClientDto> getConnectedClientsByLocation(Long locationId) {
        if (locationId == null) return java.util.List.of();
        NetworkDevice router = deviceRepository.findByLocationId(locationId).stream()
                .filter(d -> "ROUTER".equalsIgnoreCase(d.getDeviceType()))
                .findFirst().orElse(null);
        if (router == null) return java.util.List.of();
        return getConnectedClients(router.getId());
    }

    public List<com.wificaptive.gateway.dto.GwnClientDto> getConnectedClients(Long deviceId) {
        final NetworkDevice dev = deviceId != null ? deviceRepository.findById(deviceId).orElse(null) : null;
        if (dev != null && MeshApMonitorService.isMeshAp(dev)) {
            // Mesh AP: Grandstream không thấy client của mesh -> đọc thẳng từ thiết bị
            MeshApSnapshotDto snap = meshApMonitorService.getSnapshot(dev, true);
            return snap.clients().stream()
                    .map(c -> new com.wificaptive.gateway.dto.GwnClientDto(
                            c.mac(), c.ip(), c.hostname(), c.ssid(), dev.getMacAddress(),
                            dev.getModel(), c.band(), c.rssi(), 0L, 0L, 0L, dev.getGwnLanPort(), false))
                    .toList();
        }
        String apMac = dev != null ? dev.getMacAddress() : null;
        String networkIdOverride = dev != null ? dev.getGwnNetworkId() : null;
        List<com.wificaptive.gateway.dto.GwnClientDto> cloudClients = (networkIdOverride != null && !networkIdOverride.isBlank())
                ? gwnCloudGatewayAdapter.getConnectedClientsFromCloud(apMac, networkIdOverride)
                : gwnCloudGatewayAdapter.getConnectedClientsFromCloud(apMac);
        if (!cloudClients.isEmpty()) {
            return cloudClients;
        }
        // GWN7002 router often returns empty on v1.0.0/client/list for clients behind LAN/Mesh APs;
        // enrich from active Captive Portal sessions + v1.0.0/client/info so admins still see live guests.
        if (guestSessionRepository != null && dev != null && dev.getLocationId() != null) {
            Instant now = Instant.now();
            Long devLocationId = dev.getLocationId();
            List<com.wificaptive.sessions.entity.GuestSession> activeSessions =
                    guestSessionRepository.findByStatusWithLocation(com.wificaptive.sessions.entity.SessionStatus.AUTHORIZED).stream()
                            .filter(s -> s.getLocation() != null
                                    && devLocationId.equals(s.getLocation().getId())
                                    && (s.getExpiresAt() == null || s.getExpiresAt().isAfter(now)))
                            .toList();
            List<com.wificaptive.gateway.dto.GwnClientDto> fallbackClients = new ArrayList<>();
            for (var s : activeSessions) {
                var live = (networkIdOverride != null && !networkIdOverride.isBlank())
                        ? gwnCloudGatewayAdapter.fetchClientInfoFromCloud(s.getMacAddress(), networkIdOverride)
                        : gwnCloudGatewayAdapter.fetchClientInfoFromCloud(s.getMacAddress());
                String host = (live.exists() && live.hostname() != null && !live.hostname().isBlank())
                        ? live.hostname()
                        : (s.getGuestName() != null ? s.getGuestName() : "Khách Wi-Fi");
                String ip = (live.exists() && live.ipv4() != null && !live.ipv4().isBlank())
                        ? live.ipv4()
                        : s.getIpAddress();
                long rx = live.exists() && live.rxBytes() > 0 ? live.rxBytes() : (s.getBytesDownloaded() != null ? s.getBytesDownloaded() : 0L);
                long tx = live.exists() && live.txBytes() > 0 ? live.txBytes() : (s.getBytesUploaded() != null ? s.getBytesUploaded() : 0L);
                long connectSec = s.getAuthorizedAt() != null ? Math.max(0L, now.getEpochSecond() - s.getAuthorizedAt().getEpochSecond()) : 0L;
                fallbackClients.add(new com.wificaptive.gateway.dto.GwnClientDto(
                        s.getMacAddress().toUpperCase(),
                        ip,
                        host,
                        "WiFi Cong Cong Hai Phong",
                        dev.getMacAddress(),
                        dev.getModel() != null ? dev.getModel() : "GWN7002",
                        live.exists() && live.wired() ? "LAN/Mesh" : "Wi-Fi",
                        null,
                        rx,
                        tx,
                        connectSec,
                        null,
                        live.exists() && live.wired()
                ));
            }
            return fallbackClients;
        }
        return cloudClients;
    }

    public com.wificaptive.gateway.dto.GwnUpgradeResult upgradeFirmware(List<Long> deviceIds, String targetFirmware, String actor) {
        List<String> macs = deviceRepository.findAllById(deviceIds).stream()
                .map(NetworkDevice::getMacAddress)
                .toList();
        if (macs.isEmpty()) {
            throw new BadRequestException("Không tìm thấy thiết bị nào hợp lệ để nâng cấp firmware");
        }

        com.wificaptive.gateway.dto.GwnUpgradeResult result = gwnCloudGatewayAdapter.upgradeFirmwareOnCloud(macs, targetFirmware);

        auditService.recordEvent(
                AuditAction.DEVICE_REBOOT,
                actor != null ? actor : "admin",
                "internal",
                "NETWORK_DEVICE",
                "BATCH",
                "Phát lệnh nâng cấp Firmware " + targetFirmware + " cho " + macs.size() + " thiết bị. Kết quả: " + result.message()
        );

        return result;
    }

    /**
     * Xử lý gói tin Webhook cảnh báo từ Grandstream GDMS / GWN Cloud theo thời gian thực
     */
    public NetworkDeviceDto processDeviceAlarmWebhook(String rawMac, String eventOrStatus, String alarmType, String message) {
        if (rawMac == null || rawMac.isBlank()) {
            throw new BadRequestException("Địa chỉ MAC thiết bị không được để trống trong webhook");
        }
        String cleanMac = rawMac.replaceAll("[^A-Fa-f0-9]", "").toUpperCase();

        NetworkDevice dev = deviceRepository.findByMacAddress(rawMac)
                .or(() -> deviceRepository.findByCleanMac(cleanMac.toLowerCase()))
                .orElse(null);

        if (dev == null) {
            log.warn("[GWN Webhook] Nhận cảnh báo cho thiết bị chưa có trong hệ thống: MAC={}, Event={}", rawMac, eventOrStatus);
            return null;
        }

        boolean isOffline = "OFFLINE".equalsIgnoreCase(eventOrStatus) ||
                "DISCONNECTED".equalsIgnoreCase(eventOrStatus) ||
                "device_offline".equalsIgnoreCase(eventOrStatus) ||
                (alarmType != null && alarmType.toUpperCase().contains("OFFLINE"));

        boolean isOnline = "ONLINE".equalsIgnoreCase(eventOrStatus) ||
                "CONNECTED".equalsIgnoreCase(eventOrStatus) ||
                "device_online".equalsIgnoreCase(eventOrStatus) ||
                (alarmType != null && alarmType.toUpperCase().contains("ONLINE"));

        if (isOffline) {
            dev.setStatus("OFFLINE");
            dev.setLastSeenAt(Instant.now());
            NetworkDevice saved = deviceRepository.save(dev);

            String locName = dev.getLocation() != null ? dev.getLocation().getName() : "Điểm Wi-Fi";
            incidentTicketService.recordDeviceFailureIncident(
                    dev.getLocation() != null ? dev.getLocation().getId() : null,
                    dev.getId(),
                    dev.getModel() + " - " + locName,
                    dev.getMacAddress(),
                    message != null ? message : ("GDMS Webhook báo sự cố: " + (alarmType != null ? alarmType : "DEVICE_OFFLINE"))
            );

            auditService.recordEvent(
                    AuditAction.LOCATION_UPDATE,
                    "GDMS_WEBHOOK",
                    "webhook",
                    "NETWORK_DEVICE",
                    saved.getId().toString(),
                    "Phát hiện sự cố phần cứng qua GDMS Cloud Webhook: Thiết bị MAC " + rawMac + " chuyển sang OFFLINE"
            );

            return NetworkDeviceDto.from(saved);
        } else if (isOnline) {
            dev.setStatus("ONLINE");
            dev.setLastSeenAt(Instant.now());
            NetworkDevice saved = deviceRepository.save(dev);

            // Tự động khôi phục và đóng phiếu sự cố
            incidentTicketService.autoResolveRecoveredIncidents();

            auditService.recordEvent(
                    AuditAction.LOCATION_UPDATE,
                    "GDMS_WEBHOOK",
                    "webhook",
                    "NETWORK_DEVICE",
                    saved.getId().toString(),
                    "Thiết bị MAC " + rawMac + " đã khôi phục ONLINE qua GDMS Cloud Webhook"
            );

            return NetworkDeviceDto.from(saved);
        }

        return NetworkDeviceDto.from(dev);
    }
}
