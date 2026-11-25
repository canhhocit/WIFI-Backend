package com.wificaptive.incidents.service;

import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.incidents.dto.*;
import com.wificaptive.incidents.entity.CitizenIssueReport;
import com.wificaptive.incidents.entity.IncidentTicket;
import com.wificaptive.incidents.entity.IncidentUpdate;
import com.wificaptive.incidents.repository.CitizenIssueReportRepository;
import com.wificaptive.incidents.repository.IncidentTicketRepository;
import com.wificaptive.incidents.repository.IncidentUpdateRepository;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class IncidentTicketService {

    private static final Logger log = LoggerFactory.getLogger(IncidentTicketService.class);
    private static final AtomicInteger COUNTER = new AtomicInteger(100);

    private final IncidentTicketRepository ticketRepository;
    private final IncidentUpdateRepository updateRepository;
    private final CitizenIssueReportRepository citizenReportRepository;
    private final WifiLocationRepository locationRepository;
    private final com.wificaptive.devices.repository.NetworkDeviceRepository networkDeviceRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wificaptive.geography.service.AdminScopeService adminScopeService;

    public IncidentTicketService(
            IncidentTicketRepository ticketRepository,
            IncidentUpdateRepository updateRepository,
            CitizenIssueReportRepository citizenReportRepository,
            WifiLocationRepository locationRepository,
            com.wificaptive.devices.repository.NetworkDeviceRepository networkDeviceRepository) {
        this.ticketRepository = ticketRepository;
        this.updateRepository = updateRepository;
        this.citizenReportRepository = citizenReportRepository;
        this.locationRepository = locationRepository;
        this.networkDeviceRepository = networkDeviceRepository;
    }

    public void setAdminScopeService(com.wificaptive.geography.service.AdminScopeService adminScopeService) {
        this.adminScopeService = adminScopeService;
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentTicketDto> getTickets(String status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        java.util.Set<Long> scopedLocationIds = adminScopeService != null ? adminScopeService.getCurrentUserScopedLocationIds() : null;
        boolean hasStatusFilter = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status);
        Page<IncidentTicket> p;

        if (scopedLocationIds != null) {
            if (scopedLocationIds.isEmpty()) {
                p = Page.empty(pr);
            } else if (hasStatusFilter) {
                p = ticketRepository.findByStatusAndLocationIdIn(status.trim().toUpperCase(), scopedLocationIds, pr);
            } else {
                p = ticketRepository.findByLocationIdIn(scopedLocationIds, pr);
            }
        } else {
            if (hasStatusFilter) {
                p = ticketRepository.findByStatus(status.trim().toUpperCase(), pr);
            } else {
                p = ticketRepository.findAll(pr);
            }
        }

        Map<Long, String> locationNameMap = locationRepository.findAll().stream()
                .collect(Collectors.toMap(WifiLocation::getId, WifiLocation::getName, (a, b) -> a));

        return PageResponse.from(p.map(t -> {
            String locName = t.getLocationId() != null ? locationNameMap.getOrDefault(t.getLocationId(), "NVH #" + t.getLocationId()) : "Chưa xác định";
            return IncidentTicketDto.from(t, locName, null);
        }));
    }

    @Transactional(readOnly = true)
    public IncidentTicketDto getTicketById(Long id) {
        IncidentTicket t = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentTicket not found with id: " + id));
        String locName = t.getLocationId() != null
                ? locationRepository.findById(t.getLocationId()).map(WifiLocation::getName).orElse("NVH #" + t.getLocationId())
                : "Chưa xác định";
        List<IncidentUpdateDto> updates = updateRepository.findByTicketIdOrderByCreatedAtAsc(id)
                .stream().map(IncidentUpdateDto::from).collect(Collectors.toList());
        return IncidentTicketDto.from(t, locName, updates);
    }

    private synchronized String generateUniqueTicketCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "SC-" + datePart + "-";
        String candidate;
        do {
            candidate = String.format("%s%04d", prefix, COUNTER.incrementAndGet());
        } while (ticketRepository.existsByTicketCode(candidate));
        return candidate;
    }

    @Transactional
    public IncidentTicketDto createTicket(CreateIncidentRequest req, String actor) {
        String code = generateUniqueTicketCode();

        int slaHours = req.getSlaHours() != null ? req.getSlaHours() :
                "CRITICAL".equalsIgnoreCase(req.getSeverity()) ? 4 :
                "MAJOR".equalsIgnoreCase(req.getSeverity()) ? 8 : 24;

        Instant deadline = Instant.now().plus(slaHours, ChronoUnit.HOURS);

        IncidentTicket t = new IncidentTicket();
        t.setTicketCode(code);
        t.setLocationId(req.getLocationId());
        t.setDeviceId(req.getDeviceId());
        t.setIncidentType(req.getIncidentType());
        t.setSeverity(req.getSeverity() != null ? req.getSeverity() : "MAJOR");
        t.setStatus(req.getAssignedTo() != null ? "ASSIGNED" : "NEW");
        t.setTitle(req.getTitle().trim());
        t.setDescription(req.getDescription());
        t.setAssignedTo(req.getAssignedTo());
        t.setSlaDeadline(deadline);

        IncidentTicket saved = ticketRepository.save(t);

        updateRepository.save(new IncidentUpdate(
                saved.getId(),
                actor != null ? actor : "system",
                "CREATE",
                "Khởi tạo phiếu báo sự cố với SLA: " + slaHours + " giờ"
        ));

        // Nếu tạo từ phản ánh người dân, liên kết trực tiếp phản ánh đó vào phiếu sự cố này
        if (req.getCitizenReportId() != null) {
            citizenReportRepository.findById(req.getCitizenReportId()).ifPresent(report -> {
                report.setIncidentTicketId(saved.getId());
                report.setStatus("AGGREGATED_TO_INCIDENT");
                citizenReportRepository.save(report);
                log.info("Linked citizen report {} to incident ticket {}", report.getId(), saved.getTicketCode());
            });
        }

        String locName = saved.getLocationId() != null
                ? locationRepository.findById(saved.getLocationId()).map(WifiLocation::getName).orElse("Điểm #" + saved.getLocationId())
                : "Chưa gán điểm";

        log.info("Created incident ticket {} for locationId: {}", saved.getTicketCode(), saved.getLocationId());
        return IncidentTicketDto.from(saved, locName, Collections.emptyList());
    }

    @Transactional
    public IncidentTicketDto assignTicket(Long id, AssignIncidentRequest req, String actor) {
        IncidentTicket t = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentTicket not found with id: " + id));

        t.setAssignedTo(req.getAssignedTo().trim());
        t.setStatus("ASSIGNED");
        if (req.getAdditionalHours() != null && req.getAdditionalHours() > 0) {
            t.setSlaDeadline(t.getSlaDeadline().plus(req.getAdditionalHours(), ChronoUnit.HOURS));
        }

        IncidentTicket saved = ticketRepository.save(t);

        updateRepository.save(new IncidentUpdate(
                saved.getId(),
                actor != null ? actor : "dispatcher",
                "ASSIGN",
                "Phân công xử lý cho " + req.getAssignedTo() + (req.getNotes() != null ? ": " + req.getNotes() : "")
        ));

        log.info("Ticket {} assigned to {}", saved.getTicketCode(), req.getAssignedTo());
        return IncidentTicketDto.from(saved);
    }

    @Transactional
    public IncidentTicketDto resolveTicket(Long id, ResolveIncidentRequest req, String actor) {
        IncidentTicket t = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentTicket not found with id: " + id));

        t.setStatus("RESOLVED");
        t.setResolvedAt(Instant.now());
        t.setResolutionNotes(req.getResolutionNotes().trim());

        IncidentTicket saved = ticketRepository.save(t);

        updateRepository.save(new IncidentUpdate(
                saved.getId(),
                actor != null ? actor : "technician",
                "RESOLVE",
                "Khắc phục sự cố: " + req.getResolutionNotes()
        ));

        log.info("Ticket {} marked as RESOLVED by {}", saved.getTicketCode(), actor);
        return IncidentTicketDto.from(saved);
    }

    @Transactional
    public IncidentTicketDto closeTicket(Long id, String note, String actor) {
        IncidentTicket t = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentTicket not found with id: " + id));

        t.setStatus("CLOSED");
        if (t.getResolvedAt() == null) {
            t.setResolvedAt(Instant.now());
        }

        IncidentTicket saved = ticketRepository.save(t);

        updateRepository.save(new IncidentUpdate(
                saved.getId(),
                actor != null ? actor : "admin",
                "CLOSE",
                StringUtils.hasText(note) ? note.trim() : "Xác nhận nghiệm thu và hoàn tất đóng phiếu sự cố kỹ thuật."
        ));

        log.info("Ticket {} marked as CLOSED by {}", saved.getTicketCode(), actor);
        String locName = saved.getLocationId() != null
                ? locationRepository.findById(saved.getLocationId()).map(WifiLocation::getName).orElse("NVH #" + saved.getLocationId())
                : "Chưa xác định";
        List<IncidentUpdateDto> updates = updateRepository.findByTicketIdOrderByCreatedAtAsc(id)
                .stream().map(IncidentUpdateDto::from).collect(Collectors.toList());
        return IncidentTicketDto.from(saved, locName, updates);
    }

    @Transactional
    public IncidentTicketDto reopenTicket(Long id, String reason, String actor) {
        IncidentTicket t = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentTicket not found with id: " + id));

        t.setStatus("IN_PROGRESS");
        t.setResolvedAt(null);

        IncidentTicket saved = ticketRepository.save(t);

        updateRepository.save(new IncidentUpdate(
                saved.getId(),
                actor != null ? actor : "admin",
                "REOPEN",
                StringUtils.hasText(reason) ? reason.trim() : "Mở lại phiếu sự cố do hiện trường chưa khôi phục hoàn toàn."
        ));

        log.info("Ticket {} reopened by {}", saved.getTicketCode(), actor);
        String locName = saved.getLocationId() != null
                ? locationRepository.findById(saved.getLocationId()).map(WifiLocation::getName).orElse("NVH #" + saved.getLocationId())
                : "Chưa xác định";
        List<IncidentUpdateDto> updates = updateRepository.findByTicketIdOrderByCreatedAtAsc(id)
                .stream().map(IncidentUpdateDto::from).collect(Collectors.toList());
        return IncidentTicketDto.from(saved, locName, updates);
    }

    
    @Transactional
    public int autoResolveRecoveredIncidents() {
        // Tìm các phiếu sự cố đang mở (NEW, ASSIGNED, IN_PROGRESS)
        List<IncidentTicket> openTickets = ticketRepository.findAll().stream()
                .filter(t -> List.of("NEW", "ASSIGNED", "IN_PROGRESS").contains(t.getStatus()))
                .filter(t -> t.getLocationId() != null)
                .collect(Collectors.toList());

        int resolvedCount = 0;
        for (IncidentTicket ticket : openTickets) {
            List<com.wificaptive.devices.entity.NetworkDevice> devices = 
                    networkDeviceRepository.findByLocationId(ticket.getLocationId());
            if (devices.isEmpty()) {
                continue;
            }

            // Kiểm tra: nếu tất cả thiết bị tại điểm này đều đang ONLINE
            boolean allOnline = devices.stream().allMatch(d -> "ONLINE".equalsIgnoreCase(d.getStatus()));
            if (allOnline) {
                ticket.setStatus("RESOLVED");
                ticket.setResolvedAt(Instant.now());
                ticket.setResolutionNotes("Hệ thống tự động đóng phiếu sự cố do tất cả thiết bị phần cứng tại điểm đã khôi phục kết nối ONLINE (Tự động phát hiện và đóng sự cố).");
                ticketRepository.save(ticket);

                updateRepository.save(new IncidentUpdate(
                        ticket.getId(),
                        "system_auto_heal",
                        "AUTO_RESOLVE",
                        "Tự động đóng phiếu: Toàn bộ " + devices.size() + " thiết bị mạng tại điểm đã hồi phục tín hiệu ONLINE."
                ));

                log.info("Tự động đóng phiếu {} cho địa điểm id={}", ticket.getTicketCode(), ticket.getLocationId());
                resolvedCount++;
            }
        }
        return resolvedCount;
    }

    /**
     * Tự động đóng (RESOLVED) các phiếu sự cố PHẦN CỨNG tự tạo của ĐÚNG một thiết bị khi thiết bị đó
     * ONLINE trở lại. Dùng cho mesh định danh theo MAC (deviceId = id mesh) — chính xác từng AP, không
     * phải chờ cả điểm khôi phục như {@link #autoResolveRecoveredIncidents()}.
     */
    @Transactional
    public int resolveDeviceRecoveredIncidents(Long deviceId, String note) {
        if (deviceId == null) return 0;
        List<IncidentTicket> open = ticketRepository.findByDeviceIdAndStatusIn(
                deviceId, List.of("NEW", "ASSIGNED", "IN_PROGRESS"));
        int n = 0;
        for (IncidentTicket t : open) {
            // Chỉ đụng phiếu phần cứng do hệ thống tự tạo, không tự đóng phiếu nghiệp vụ khác.
            if (!"HARDWARE_FAULT".equalsIgnoreCase(t.getIncidentType())) continue;
            t.setStatus("RESOLVED");
            t.setResolvedAt(Instant.now());
            t.setResolutionNotes(StringUtils.hasText(note) ? note.trim()
                    : "Hệ thống tự động đóng: thiết bị đã khôi phục kết nối ONLINE.");
            ticketRepository.save(t);
            updateRepository.save(new IncidentUpdate(t.getId(), "system_auto_heal", "AUTO_RESOLVE",
                    StringUtils.hasText(note) ? note.trim() : "Thiết bị đã ONLINE trở lại — tự động đóng phiếu."));
            log.info("[AUTO RESOLVE] Phiếu {} tự đóng do thiết bị deviceId={} ONLINE trở lại", t.getTicketCode(), deviceId);
            n++;
        }
        return n;
    }

    /**
     * Tự động đóng phiếu "AP cổng LANx mất kết nối" khi cổng LAN đó UP trở lại (mô hình AP-theo-cổng:
     * phiếu gắn deviceId = router GWN, phân biệt từng cổng theo tiêu đề có "LANx").
     */
    @Transactional
    public int resolvePortRecoveredIncidents(Long gwnDeviceId, Long locationId, String port, String note) {
        List<String> openStatuses = List.of("NEW", "ASSIGNED", "IN_PROGRESS");
        List<IncidentTicket> candidates = gwnDeviceId != null
                ? ticketRepository.findByDeviceIdAndStatusIn(gwnDeviceId, openStatuses)
                : (locationId != null ? ticketRepository.findByLocationIdAndStatusIn(locationId, openStatuses)
                                      : Collections.emptyList());
        String marker = "LAN" + port + " "; // khoảng trắng cuối để "LAN4" không khớp "LAN41"
        int n = 0;
        for (IncidentTicket t : candidates) {
            if (!"HARDWARE_FAULT".equalsIgnoreCase(t.getIncidentType())) continue;
            if (t.getTitle() == null || !t.getTitle().contains(marker)) continue;
            t.setStatus("RESOLVED");
            t.setResolvedAt(Instant.now());
            t.setResolutionNotes(StringUtils.hasText(note) ? note.trim()
                    : "Hệ thống tự động đóng: cổng LAN" + port + " đã UP trở lại (AP được cắm/khôi phục).");
            ticketRepository.save(t);
            updateRepository.save(new IncidentUpdate(t.getId(), "system_gwn_port_monitor", "AUTO_RESOLVE",
                    "Cổng LAN" + port + " đã UP trở lại — tự động đóng phiếu."));
            log.info("[AUTO RESOLVE] Phiếu {} tự đóng do cổng LAN{} UP trở lại", t.getTicketCode(), port);
            n++;
        }
        return n;
    }

    @Transactional
    public CitizenReportResponse recordCitizenReport(CreateCitizenReportRequest req, String remoteIp) {
        // 1. Phân giải điểm phát sóng theo locationId hoặc slug
        WifiLocation location = null;
        if (req.getLocationId() != null) {
            location = locationRepository.findById(req.getLocationId()).orElse(null);
        }
        if (location == null && StringUtils.hasText(req.getLocationSlug())) {
            location = locationRepository.findBySlugAndDeletedFalse(req.getLocationSlug().trim())
                    .or(() -> locationRepository.findBySlug(req.getLocationSlug().trim()))
                    .orElse(null);
        }
        if (location == null) {
            // Fallback lấy điểm phát sóng mẫu nếu không truyền rõ
            List<WifiLocation> activeLocs = locationRepository.findByActiveTrueAndDeletedFalse();
            if (!activeLocs.isEmpty()) {
                location = activeLocs.get(0);
            }
        }

        Long locationId = location != null ? location.getId() : null;
        String locationName = location != null ? location.getName() : "Điểm Wi-Fi Cộng đồng";

        String clientMac = StringUtils.hasText(req.getClientMac()) ? req.getClientMac().trim().toLowerCase() : null;
        String clientIp = StringUtils.hasText(req.getClientIp()) ? req.getClientIp().trim() : remoteIp;

        // 2. Lưu phản ánh vào CSDL
        CitizenIssueReport report = new CitizenIssueReport();
        report.setLocationId(locationId);
        report.setClientMac(clientMac);
        report.setClientIp(clientIp);
        report.setPhoneNumber(StringUtils.hasText(req.getPhoneNumber()) ? req.getPhoneNumber().trim() : null);
        report.setReporterName(StringUtils.hasText(req.getReporterName()) ? req.getReporterName().trim() : null);
        report.setIssueType(StringUtils.hasText(req.getIssueType()) ? req.getIssueType().trim() : "OTHER");
        report.setDescription(StringUtils.hasText(req.getDescription()) ? req.getDescription().trim() : null);
        report.setStatus("PENDING");
        report = citizenReportRepository.save(report);

        log.info("[Citizen Report] Nhận phản ánh mới ID #{}: type={}, location={}, phone={}",
                report.getId(), report.getIssueType(), locationName, report.getPhoneNumber());

        boolean incidentCreated = false;
        String ticketCode = null;

        if (locationId != null) {
            Instant thirtyMinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
            List<CitizenIssueReport> pendingReports = citizenReportRepository
                    .findByLocationIdAndCreatedAtAfterAndIncidentTicketIdIsNullOrderByCreatedAtAsc(locationId, thirtyMinutesAgo);

            if (pendingReports.size() >= 3) {
                // Kiểm tra xem đã có phiếu sự cố đang mở (NEW, ASSIGNED, IN_PROGRESS) cho điểm này chưa
                List<IncidentTicket> activeTickets = ticketRepository
                        .findByLocationIdAndStatusIn(locationId, List.of("NEW", "ASSIGNED", "IN_PROGRESS"));

                IncidentTicket targetTicket;
                if (activeTickets.isEmpty()) {
                    String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    String newCode = String.format("SC-%s-%04d", datePart, COUNTER.incrementAndGet());
                    Instant deadline = Instant.now().plus(8, ChronoUnit.HOURS); // MAJOR SLA: 8 giờ

                    targetTicket = new IncidentTicket();
                    targetTicket.setTicketCode(newCode);
                    targetTicket.setLocationId(locationId);
                    targetTicket.setIncidentType("SYSTEM_DEGRADATION");
                    targetTicket.setSeverity("MAJOR");
                    targetTicket.setStatus("NEW");
                    targetTicket.setTitle("Sự cố mạng tại " + locationName + " (Tự động kích hoạt từ 3+ phản ánh người dân trong 30 phút)");
                    targetTicket.setDescription(String.format(
                            "Hệ thống tự động kích hoạt phiếu sự cố: Tiếp nhận %d phản ánh từ người dân trong vòng 30 phút tại %s. Các phản ánh chính: %s.",
                            pendingReports.size(),
                            locationName,
                            pendingReports.stream().map(CitizenIssueReport::getIssueType).distinct().collect(Collectors.joining(", "))
                    ));
                    targetTicket.setSlaDeadline(deadline);
                    targetTicket = ticketRepository.save(targetTicket);

                    updateRepository.save(new IncidentUpdate(
                            targetTicket.getId(),
                            "system_auto_trigger",
                            "AUTO_TRIGGER",
                            String.format("Tự động kích hoạt phiếu sự cố từ %d phản ánh người dân trong 30 phút tại %s.",
                                    pendingReports.size(), locationName)
                    ));

                    incidentCreated = true;
                    ticketCode = targetTicket.getTicketCode();
                    log.warn("[Citizen Report] Tự động tạo phiếu sự cố {} cho điểm {} từ {} phản ánh người dân trong 30 phút",
                            ticketCode, locationName, pendingReports.size());
                } else {
                    targetTicket = activeTickets.get(0);
                    ticketCode = targetTicket.getTicketCode();
                    updateRepository.save(new IncidentUpdate(
                            targetTicket.getId(),
                            "citizen_portal",
                            "APPEND_REPORT",
                            String.format("Tiếp nhận thêm phản ánh chất lượng từ người dân (Loại: %s).", report.getIssueType())
                    ));
                }

                // Gán tất cả phản ánh chờ trong 30 phút vào phiếu sự cố này
                Long ticketId = targetTicket.getId();
                for (CitizenIssueReport r : pendingReports) {
                    r.setIncidentTicketId(ticketId);
                    r.setStatus("AGGREGATED_TO_INCIDENT");
                    citizenReportRepository.save(r);
                }
            }
        }

        String confirmMessage = incidentCreated
                ? "Cảm ơn bà con đã phản ánh. Hệ thống đã tự động ghi nhận phiếu sự cố kỹ thuật số " + ticketCode + " để phân công kỹ thuật viên kiểm tra xử lý ngay."
                : "Cảm ơn bà con đã phản ánh chất lượng dịch vụ. Ý kiến đã được ghi nhận và gửi đến bộ phận kỹ thuật phụ trách tại " + locationName + ".";

        return new CitizenReportResponse(report.getId(), incidentCreated, ticketCode, locationName, confirmMessage);
    }

    @Transactional(readOnly = true)
    public PageResponse<CitizenReportDto> getCitizenReports(Long locationId, String status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CitizenIssueReport> p;
        if (locationId != null) {
            p = citizenReportRepository.findByLocationIdOrderByCreatedAtDesc(locationId, pr);
        } else if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            p = citizenReportRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(), pr);
        } else {
            p = citizenReportRepository.findAllByOrderByCreatedAtDesc(pr);
        }

        // Cache locations & tickets names to avoid N+1 queries
        Map<Long, String> locationNameMap = locationRepository.findAll().stream()
                .collect(Collectors.toMap(WifiLocation::getId, WifiLocation::getName, (a, b) -> a));
        Map<Long, String> ticketCodeMap = ticketRepository.findAll().stream()
                .collect(Collectors.toMap(IncidentTicket::getId, IncidentTicket::getTicketCode, (a, b) -> a));

        return PageResponse.from(p.map(r -> {
            String locName = r.getLocationId() != null ? locationNameMap.getOrDefault(r.getLocationId(), "Điểm #" + r.getLocationId()) : "Chưa xác định";
            String code = r.getIncidentTicketId() != null ? ticketCodeMap.getOrDefault(r.getIncidentTicketId(), null) : null;
            return CitizenReportDto.from(r, locName, code);
        }));
    }

    /**
     * Tự động tạo phiếu sự cố phần cứng khi thiết bị GWN/Router bị mất kết nối (OFFLINE)
     * Ngăn chặn tạo phiếu trùng lặp nếu thiết bị hoặc địa điểm này đang có phiếu mở.
     */
    @Transactional
    public IncidentTicketDto recordDeviceFailureIncident(Long locationId, Long deviceId, String deviceName, String macAddress, String reason) {
        List<String> openStatuses = List.of("NEW", "ASSIGNED", "IN_PROGRESS");
        if (deviceId != null) {
            List<IncidentTicket> existing = ticketRepository.findByDeviceIdAndStatusIn(deviceId, openStatuses);
            if (!existing.isEmpty()) {
                log.debug("Thiết bị deviceId={} đã có phiếu sự cố đang mở: {}", deviceId, existing.get(0).getTicketCode());
                return IncidentTicketDto.from(existing.get(0));
            }
        } else if (locationId != null) {
            List<IncidentTicket> existing = ticketRepository.findByLocationIdAndStatusIn(locationId, openStatuses);
            if (!existing.isEmpty()) {
                log.debug("Địa điểm locationId={} đã có phiếu sự cố đang mở: {}", locationId, existing.get(0).getTicketCode());
                return IncidentTicketDto.from(existing.get(0));
            }
        }

        String locTitle = "Chưa gán điểm";
        if (locationId != null) {
            locTitle = locationRepository.findById(locationId).map(WifiLocation::getName).orElse("Điểm #" + locationId);
        }

        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setLocationId(locationId);
        req.setDeviceId(deviceId);
        req.setIncidentType("HARDWARE_FAULT");
        req.setSeverity("CRITICAL");
        req.setSlaHours(4); // Cam kết khôi phục 4 giờ theo chuẩn SLA Sở TT&TT
        req.setTitle("Sự cố mất kết nối phần cứng: " + (deviceName != null ? deviceName : macAddress));
        req.setDescription(String.format(
                "Hệ thống tự động phát hiện thiết bị %s (MAC: %s) tại [%s] đã chuyển sang trạng thái OFFLINE. Lý do: %s. Đề nghị kỹ thuật viên kiểm tra nguồn điện và kết nối cáp quang.",
                deviceName != null ? deviceName : "AP",
                macAddress != null ? macAddress : "N/A",
                locTitle,
                reason != null ? reason : "Mất tín hiệu heartbeat hoặc đứt cáp quang FTTH"
        ));

        IncidentTicketDto ticket = createTicket(req, "system_gwn_monitor");
        log.warn("[AUTO INCIDENT] Tự động tạo phiếu sự cố {} cho thiết bị {} tại {}", ticket.getTicketCode(), macAddress, locTitle);
        return ticket;
    }
}

