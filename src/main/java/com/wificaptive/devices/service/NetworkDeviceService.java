package com.wificaptive.devices.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.devices.dto.DeviceSwapRequest;
import com.wificaptive.devices.dto.NetworkDeviceDto;
import com.wificaptive.devices.entity.DeviceSwapLog;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.DeviceSwapLogRepository;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NetworkDeviceService {

    private final NetworkDeviceRepository deviceRepository;
    private final DeviceSwapLogRepository swapLogRepository;
    private final WifiLocationRepository locationRepository;
    private final AuditService auditService;

    public NetworkDeviceService(NetworkDeviceRepository deviceRepository,
                                DeviceSwapLogRepository swapLogRepository,
                                WifiLocationRepository locationRepository,
                                AuditService auditService) {
        this.deviceRepository = deviceRepository;
        this.swapLogRepository = swapLogRepository;
        this.locationRepository = locationRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<NetworkDeviceDto> getDevices(String type, String status, Long adminUnitId, Pageable pageable) {
        Page<NetworkDevice> page;
        if (type != null && !type.isBlank()) {
            page = deviceRepository.findByDeviceType(type.toUpperCase(), pageable);
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
}
