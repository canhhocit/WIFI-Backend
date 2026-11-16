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

    @Transactional(readOnly = true)
    public PageResponse<IncidentTicketDto> getTickets(String status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<IncidentTicket> p;
        if (status != null && !status.isBlank()) {
            p = ticketRepository.findByStatus(status.trim().toUpperCase(), pr);
        } else {
            p = ticketRepository.findAll(pr);
        }
        return PageResponse.from(p.map(IncidentTicketDto::from));
    }

    @Transactional(readOnly = true)
    public IncidentTicketDto getTicketById(Long id) {
        IncidentTicket t = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentTicket not found with id: " + id));
        return IncidentTicketDto.from(t);
    }

    @Transactional
    public IncidentTicketDto createTicket(CreateIncidentRequest req, String actor) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code = String.format("SC-%s-%04d", datePart, COUNTER.incrementAndGet());

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

        log.info("Created incident ticket {} for locationId: {}", saved.getTicketCode(), saved.getLocationId());
        return IncidentTicketDto.from(saved);
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
}

