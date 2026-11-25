package com.wificaptive.devices.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.devices.dto.DeviceDto;
import com.wificaptive.devices.entity.Device;
import com.wificaptive.devices.repository.DeviceRepository;
import com.wificaptive.sessions.entity.GuestSession;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import com.wificaptive.sessions.service.GuestSessionService;
import com.wificaptive.surveys.entity.SurveyResponse;
import com.wificaptive.surveys.repository.SurveyResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final AuditService auditService;
    private final GuestSessionService guestSessionService;

    // Nguồn thông tin khách THẬT từ cổng chào (tùy chọn để không phá vỡ test dùng constructor cũ).
    @Autowired(required = false)
    private GuestSessionRepository guestSessionRepository;
    @Autowired(required = false)
    private SurveyResponseRepository surveyResponseRepository;
    @Autowired(required = false)
    private com.wificaptive.surveys.repository.SurveyQuestionRepository surveyQuestionRepository;
    @Autowired(required = false)
    private com.wificaptive.surveys.repository.SurveyRepository surveyRepository;

    private static final com.fasterxml.jackson.databind.ObjectMapper DETAIL_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

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
        PageResponse<DeviceDto> resp = PageResponse.from(page.map(DeviceDto::from));
        enrichWithGuestInfo(resp.getContent());
        return resp;
    }

    /**
     * Gắn thông tin khách (họ tên/liên hệ/điểm/phiên đang hoạt động) vào DTO thiết bị từ GuestSession
     * mới nhất theo MAC. Batch 1 truy vấn cho cả trang (không N+1). Bỏ qua nếu chưa có repository.
     */
    private void enrichWithGuestInfo(List<DeviceDto> dtos) {
        if (guestSessionRepository == null || dtos == null || dtos.isEmpty()) return;
        Map<String, DeviceDto> byMac = new HashMap<>();
        for (DeviceDto d : dtos) {
            if (d.getMacAddress() != null) byMac.put(d.getMacAddress().toLowerCase(Locale.ROOT), d);
        }
        if (byMac.isEmpty()) return;
        List<GuestSession> sessions = guestSessionRepository.findByMacLowerInOrderByCreatedAtDesc(byMac.keySet());
        Instant now = Instant.now();
        Set<String> latestDone = new HashSet<>();
        for (GuestSession s : sessions) {
            String mac = s.getMacAddress() != null ? s.getMacAddress().toLowerCase(Locale.ROOT) : null;
            DeviceDto d = mac != null ? byMac.get(mac) : null;
            if (d == null) continue;
            // Danh sách đã sắp mới nhất trước -> bản ghi đầu tiên của mỗi MAC là phiên mới nhất.
            if (latestDone.add(mac)) {
                d.setGuestName(s.getGuestName());
                d.setGuestContact(s.getGuestContact());
                d.setLastLocationName(s.getLocation() != null ? s.getLocation().getName() : null);
            }
            if (!d.isHasActiveSession()
                    && s.getStatus() == SessionStatus.AUTHORIZED
                    && s.getExpiresAt() != null && s.getExpiresAt().isAfter(now)) {
                d.setHasActiveSession(true);
            }
        }
    }

    /**
     * Chi tiết thiết bị khách: thông tin thiết bị + LỊCH SỬ PHIÊN cổng chào + CÂU TRẢ LỜI KHẢO SÁT
     * (đều gắn theo MAC). Đây là nguồn "xem chi tiết" thật, không phụ thuộc Grandstream.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDeviceDetail(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", id));
        DeviceDto dto = DeviceDto.from(device);
        enrichWithGuestInfo(List.of(dto));

        String macLower = device.getMacAddress() != null ? device.getMacAddress().toLowerCase(Locale.ROOT) : "";
        List<Map<String, Object>> sessions = new ArrayList<>();
        if (guestSessionRepository != null && !macLower.isEmpty()) {
            for (GuestSession s : guestSessionRepository.findByMacLowerInOrderByCreatedAtDesc(List.of(macLower))) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("locationName", s.getLocation() != null ? s.getLocation().getName() : null);
                // "Guest User" là tên mặc định khi khách không khai -> trả null để giao diện hiện "khách vãng lai".
                String gn = s.getGuestName();
                m.put("guestName", (gn == null || gn.isBlank() || "Guest User".equalsIgnoreCase(gn.trim())) ? null : gn.trim());
                m.put("guestContact", s.getGuestContact());
                m.put("status", s.getStatus() != null ? s.getStatus().name() : null);
                m.put("ipAddress", s.getIpAddress());
                m.put("authorizedAt", s.getAuthorizedAt());
                m.put("expiresAt", s.getExpiresAt());
                m.put("createdAt", s.getCreatedAt());
                m.put("bytesDownloaded", s.getBytesDownloaded());
                m.put("bytesUploaded", s.getBytesUploaded());
                sessions.add(m);
            }
        }
        List<Map<String, Object>> surveys = new ArrayList<>();
        if (surveyResponseRepository != null && device.getMacAddress() != null) {
            java.util.Map<Long, java.util.List<com.wificaptive.surveys.entity.SurveyQuestion>> qCache = new HashMap<>();
            java.util.Map<Long, String> titleCache = new HashMap<>();
            for (SurveyResponse r : surveyResponseRepository.findByClientMacIgnoreCaseOrderBySubmittedAtDesc(device.getMacAddress())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("surveyId", r.getSurveyId());
                m.put("surveyTitle", resolveSurveyTitle(r.getSurveyId(), titleCache));
                m.put("submittedAt", r.getSubmittedAt());
                m.put("answers", resolveSurveyAnswers(r, qCache)); // [{question, answer}] đã dịch đẹp
                surveys.add(m);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("device", dto);
        out.put("sessions", sessions);
        out.put("surveys", surveys);
        return out;
    }

    private String resolveSurveyTitle(Long surveyId, java.util.Map<Long, String> cache) {
        if (surveyId == null || surveyRepository == null) return null;
        return cache.computeIfAbsent(surveyId, id ->
                surveyRepository.findById(id).map(com.wificaptive.surveys.entity.Survey::getTitle).orElse("Khảo sát #" + id));
    }

    /** Dịch answersJson (questionId -> giá trị) thành danh sách {question, answer} đọc được theo thứ tự câu hỏi. */
    private List<Map<String, Object>> resolveSurveyAnswers(
            SurveyResponse r, java.util.Map<Long, java.util.List<com.wificaptive.surveys.entity.SurveyQuestion>> qCache) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (r.getSurveyId() == null || surveyQuestionRepository == null) return out;
        Map<String, Object> ans;
        try {
            ans = DETAIL_MAPPER.readValue(r.getAnswersJson() == null ? "{}" : r.getAnswersJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            ans = java.util.Collections.emptyMap();
        }
        java.util.List<com.wificaptive.surveys.entity.SurveyQuestion> questions = qCache.computeIfAbsent(
                r.getSurveyId(), id -> surveyQuestionRepository.findBySurveyIdOrderBySortOrderAsc(id));
        for (com.wificaptive.surveys.entity.SurveyQuestion q : questions) {
            Object val = ans.get(String.valueOf(q.getId()));
            if (val == null) continue;
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("question", q.getQuestionText());
            a.put("answer", formatSurveyAnswer(q.getQuestionType(), val));
            out.add(a);
        }
        return out;
    }

    private String formatSurveyAnswer(String questionType, Object val) {
        if (val instanceof java.util.List<?> list) {
            return list.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
        }
        String s = String.valueOf(val);
        if ("RATING".equalsIgnoreCase(questionType)) {
            return s + "/5";
        }
        return s;
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
