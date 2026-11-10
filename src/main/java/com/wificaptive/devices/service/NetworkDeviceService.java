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
import com.wificaptive.devices.repository.DeviceSwapLogRepository;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter;
import com.wificaptive.gateway.dto.GwnDeviceSyncDto;
import com.wificaptive.gateway.dto.GwnRebootResult;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
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

    private final NetworkDeviceRepository deviceRepository;
    private final DeviceSwapLogRepository swapLogRepository;
    private final WifiLocationRepository locationRepository;
    private final AuditService auditService;
    private final GwnCloudGatewayAdapter gwnCloudGatewayAdapter;

    public NetworkDeviceService(NetworkDeviceRepository deviceRepository,
                                DeviceSwapLogRepository swapLogRepository,
                                WifiLocationRepository locationRepository,
                                AuditService auditService,
                                GwnCloudGatewayAdapter gwnCloudGatewayAdapter) {
        this.deviceRepository = deviceRepository;
        this.swapLogRepository = swapLogRepository;
        this.locationRepository = locationRepository;
        this.auditService = auditService;
        this.gwnCloudGatewayAdapter = gwnCloudGatewayAdapter;
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
        return PageResponse.from(page.map(NetworkDeviceDto::from));
    }

    @Transactional(readOnly = true)
    public List<NetworkDeviceDto> getDevicesByLocation(Long locationId) {
        return deviceRepository.findByLocationId(locationId).stream()
                .map(NetworkDeviceDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NetworkDeviceDto getDeviceById(Long id) {
        NetworkDevice dev = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));
        return NetworkDeviceDto.from(dev);
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
}
