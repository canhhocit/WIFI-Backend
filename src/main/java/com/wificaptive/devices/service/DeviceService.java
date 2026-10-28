package com.wificaptive.devices.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.devices.dto.DeviceDto;
import com.wificaptive.devices.entity.Device;
import com.wificaptive.devices.repository.DeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final AuditService auditService;

    public DeviceService(DeviceRepository deviceRepository, AuditService auditService) {
        this.deviceRepository = deviceRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Device registerOrUpdateDevice(String macAddress, String ipAddress, String userAgent) {
        String normalizedMac = (macAddress != null) ? macAddress.trim().toLowerCase() : "00:00:00:00:00:00";
        Optional<Device> existingOpt = deviceRepository.findByMacAddressIgnoreCase(normalizedMac);

        Device device;
        if (existingOpt.isPresent()) {
            device = existingOpt.get();
            device.setLastSeenAt(Instant.now());
            if (StringUtils.hasText(ipAddress)) {
                device.setIpAddress(ipAddress);
            }
            if (StringUtils.hasText(userAgent)) {
                device.setUserAgent(userAgent);
                device.setOsType(detectOsType(userAgent));
            }
        } else {
            device = new Device(
                    normalizedMac,
                    ipAddress,
                    "Client-" + normalizedMac.replace(":", "").substring(Math.max(0, normalizedMac.replace(":", "").length() - 4)),
                    userAgent,
                    detectOsType(userAgent)
            );
        }

        return deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public PageResponse<DeviceDto> getDevices(String search, Pageable pageable) {
        Page<Device> page;
        if (StringUtils.hasText(search)) {
            String q = search.trim();
            page = deviceRepository.findByMacAddressContainingIgnoreCaseOrIpAddressContaining(q, q, pageable);
        } else {
            page = deviceRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(DeviceDto::from));
    }

    @Transactional
    public DeviceDto toggleBlockDevice(Long id, boolean blocked, String actorUsername) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", id));
        device.setBlocked(blocked);
        Device saved = deviceRepository.save(device);

        auditService.recordEvent(
                AuditAction.DEVICE_BLOCK,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "DEVICE",
                device.getMacAddress(),
                (blocked ? "Blocked" : "Unblocked") + " device MAC: " + device.getMacAddress()
        );

        return DeviceDto.from(saved);
    }

    private String detectOsType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) return "iOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("macintosh") || ua.contains("mac os x")) return "macOS";
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("linux")) return "Linux";
        return "Other";
    }
}
