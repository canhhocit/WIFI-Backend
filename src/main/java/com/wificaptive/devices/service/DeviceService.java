package com.wificaptive.devices.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.devices.dto.DeviceDto;
import com.wificaptive.devices.entity.Device;
import com.wificaptive.devices.repository.DeviceRepository;
import com.wificaptive.sessions.service.GuestSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final AuditService auditService;
    private final GuestSessionService guestSessionService;

    // In-memory cache to prevent repetitive DB queries and row-lock updates on peak loads
    private final ConcurrentHashMap<String, Device> activeDeviceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> deviceLastPersistMap = new ConcurrentHashMap<>();

    public DeviceService(DeviceRepository deviceRepository, AuditService auditService) {
        this(deviceRepository, auditService, null);
    }

    @Autowired
    public DeviceService(
            DeviceRepository deviceRepository,
            AuditService auditService,
            @Autowired(required = false) GuestSessionService guestSessionService) {
        this.deviceRepository = deviceRepository;
        this.auditService = auditService;
        this.guestSessionService = guestSessionService;
    }

    @Transactional
    public Device registerOrUpdateDevice(String macAddress, String ipAddress, String userAgent) {
        String normalizedMac = (macAddress != null) ? macAddress.trim().toLowerCase() : "00:00:00:00:00:00";

        // ĐỀ XUẤT 2: Fast-path In-Memory. Nếu thiết bị đã cập nhật trong vòng 5 phút (300s),
        // trả về ngay từ RAM, triệt tiêu câu lệnh UPDATE CSDL và tranh chấp khóa dòng (Row-lock).
        Device cached = activeDeviceCache.get(normalizedMac);
        Instant lastPersist = deviceLastPersistMap.get(normalizedMac);
        if (cached != null && lastPersist != null && lastPersist.plusSeconds(300).isAfter(Instant.now())) {
            return cached;
        }

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

        Device saved = deviceRepository.save(device);
        activeDeviceCache.put(normalizedMac, saved);
        deviceLastPersistMap.put(normalizedMac, Instant.now());
        return saved;
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

        // Invalidate in-memory cache on block status change
        if (device.getMacAddress() != null) {
            String norm = device.getMacAddress().trim().toLowerCase();
            activeDeviceCache.remove(norm);
            deviceLastPersistMap.remove(norm);

            if (guestSessionService != null) {
                if (blocked) {
                    guestSessionService.enforceHardwareMacBlock(
                            device.getMacAddress(),
                            actorUsername,
                            "Thiết bị bị chặn từ trang Quản lý thiết bị"
                    );
                } else {
                    guestSessionService.releaseHardwareMacBlock(device.getMacAddress());
                }
            }
        }

        auditService.recordEvent(
                blocked ? AuditAction.DEVICE_BLOCK : AuditAction.DEVICE_UNBLOCK,
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
