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

    @Transactional(readOnly = true)
    public PageResponse<NetworkDeviceDto> getDevices(String type, String status, Long adminUnitId, Pageable pageable) {
        Page<NetworkDevice> page;
        if (type != null && !type.isBlank()) {
            String normType = type.trim().toUpperCase();
            if ("AP".equalsIgnoreCase(normType)) {
                normType = "ACCESS_POINT";
            }
            page = deviceRepository.findByDeviceType(normType, pageable);
        } else if (status != null && !status.isBlank()) {
            page = deviceRepository.findByStatus(status.toUpperCase(), pageable);
        } else if (adminUnitId != null) {
            page = deviceRepository.findByLocationAdminUnitId(adminUnitId, pageable);
        } else {
            page = deviceRepository.findAll(pageable);
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
    public NetworkDeviceDto getDeviceById(Long id) {
        NetworkDevice dev = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));
        return toDto(dev);
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

    public DeviceSyncSummaryDto syncFromGrandstream(String actorUsername) {
        List<GwnDeviceSyncDto> cloudDevices = gwnCloudGatewayAdapter.fetchDevicesFromCloud();
        int updatedCount = 0;
        int createdCount = 0;
        List<NetworkDeviceDto> resultList = new ArrayList<>();

        WifiLocation defaultLoc = locationRepository.findAll().stream()
                .filter(l -> !l.isDeleted())
                .findFirst()
                .orElse(null);

        for (GwnDeviceSyncDto cDev : cloudDevices) {
            String normMac = cDev.getMac().toUpperCase();
            String cleanMac = normMac.replaceAll("[^A-F0-9]", "");

            NetworkDevice dev = deviceRepository.findByMacAddress(normMac)
                    .or(() -> deviceRepository.findAll().stream()
                            .filter(d -> d.getMacAddress().replaceAll("[^A-F0-9]", "").equalsIgnoreCase(cleanMac))
                            .findFirst())
                    .orElse(null);

            if (dev != null) {
                String oldStatus = dev.getStatus();
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
                if (dev.getModel() == null || dev.getModel().isBlank()) {
                    dev.setModel(cDev.getModel());
                }
                dev.setLastSeenAt(Instant.now());
                NetworkDevice saved = deviceRepository.save(dev);
                resultList.add(NetworkDeviceDto.from(saved));
                updatedCount++;

                // Tự động kích hoạt tạo Phiếu sự cố khi thiết bị báo OFFLINE
                if ("OFFLINE".equalsIgnoreCase(cDev.getStatus())) {
                    String locName = dev.getLocation() != null ? dev.getLocation().getName() : "Nhà văn hóa";
                    incidentTicketService.recordDeviceFailureIncident(
                            dev.getLocation() != null ? dev.getLocation().getId() : null,
                            dev.getId(),
                            dev.getModel() + " - " + locName,
                            dev.getMacAddress(),
                            "Thiết bị báo mất kết nối (OFFLINE) trong phiên quét định kỳ từ Grandstream GDMS/GWN Cloud"
                    );
                }
            } else if (defaultLoc != null) {
                NetworkDevice newDev = new NetworkDevice();
                newDev.setLocation(defaultLoc);
                newDev.setDeviceType("ROUTER");
                newDev.setVendor("Grandstream");
                newDev.setModel(cDev.getModel() != null ? cDev.getModel() : "GWN7002");
                newDev.setSerialNumber(cleanMac);
                newDev.setMacAddress(normMac);
                newDev.setIpAddress(cDev.getIp());
                newDev.setStatus(cDev.getStatus());
                newDev.setFirmwareVersion(cDev.getFirmware());
                newDev.setCpuUsagePercent(cDev.getCpuPercent() != null ? cDev.getCpuPercent() : 10);
                newDev.setMemoryUsagePercent(cDev.getMemoryPercent() != null ? cDev.getMemoryPercent() : 40);
                newDev.setUptimeSeconds(cDev.getUptimeSeconds() != null ? cDev.getUptimeSeconds() : 3600L);
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
                "Đồng bộ thành công " + cloudDevices.size() + " thiết bị từ Grandstream Cloud (Cập nhật: " + updatedCount + ", Thêm mới: " + createdCount + ")"
        );

        String msg = String.format("Đồng bộ hoàn tất: %d thiết bị được cập nhật trạng thái thực tế từ Grandstream Cloud.", updatedCount + createdCount);
        return new DeviceSyncSummaryDto(cloudDevices.size(), updatedCount, createdCount, resultList, msg, !gwnCloudGatewayAdapter.isLiveCredentialsConfigured());
    }

    public NetworkDeviceDto rebootDevice(Long id, String actorUsername) {
        NetworkDevice dev = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));

        GwnRebootResult result = gwnCloudGatewayAdapter.rebootDeviceOnCloud(dev.getMacAddress());
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

    public List<com.wificaptive.gateway.dto.GwnClientDto> getConnectedClients(Long deviceId) {
        String apMac = null;
        if (deviceId != null) {
            NetworkDevice dev = deviceRepository.findById(deviceId).orElse(null);
            if (dev != null && MeshApMonitorService.isMeshAp(dev)) {
                // Mesh AP: Grandstream không thấy client của mesh -> đọc thẳng từ thiết bị
                MeshApSnapshotDto snap = meshApMonitorService.getSnapshot(dev, true);
                return snap.clients().stream()
                        .map(c -> new com.wificaptive.gateway.dto.GwnClientDto(
                                c.mac(), c.ip(), c.hostname(), c.ssid(), dev.getMacAddress(),
                                dev.getModel(), c.band(), c.rssi(), 0L, 0L, 0L))
                        .toList();
            }
            apMac = dev != null ? dev.getMacAddress() : null;
        }
        return gwnCloudGatewayAdapter.getConnectedClientsFromCloud(apMac);
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
                .or(() -> deviceRepository.findAll().stream()
                        .filter(d -> d.getMacAddress().replaceAll("[^A-Fa-f0-9]", "").equalsIgnoreCase(cleanMac))
                        .findFirst())
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
