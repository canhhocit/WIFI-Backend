package com.wificaptive.reporting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.incidents.entity.IncidentTicket;
import com.wificaptive.incidents.repository.IncidentTicketRepository;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.reporting.dto.DistrictStatDto;
import com.wificaptive.reporting.dto.GovernmentReportDto;
import com.wificaptive.reporting.dto.GovernmentReportHistoryDto;
import com.wificaptive.reporting.dto.SubmitReportRequest;
import com.wificaptive.reporting.entity.GovernmentReportHistory;
import com.wificaptive.reporting.repository.GovernmentReportHistoryRepository;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import com.wificaptive.surveys.entity.SurveyQuestion;
import com.wificaptive.surveys.entity.SurveyResponse;
import com.wificaptive.surveys.repository.SurveyQuestionRepository;
import com.wificaptive.surveys.repository.SurveyResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GovernmentReportService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final WifiLocationRepository locationRepository;
    private final GuestSessionRepository sessionRepository;
    private final IncidentTicketRepository incidentRepository;
    private final AdministrativeUnitRepository adminUnitRepository;
    private final GovernmentReportHistoryRepository historyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private NetworkDeviceRepository networkDeviceRepository;

    public GovernmentReportService(
            WifiLocationRepository locationRepository,
            GuestSessionRepository sessionRepository,
            IncidentTicketRepository incidentRepository,
            AdministrativeUnitRepository adminUnitRepository) {
        this(locationRepository, sessionRepository, incidentRepository, adminUnitRepository, null, null, null, new ObjectMapper());
    }

    public GovernmentReportService(
            WifiLocationRepository locationRepository,
            GuestSessionRepository sessionRepository,
            IncidentTicketRepository incidentRepository,
            AdministrativeUnitRepository adminUnitRepository,
            GovernmentReportHistoryRepository historyRepository) {
        this(locationRepository, sessionRepository, incidentRepository, adminUnitRepository, historyRepository, null, null, new ObjectMapper());
    }

    public GovernmentReportService(
            WifiLocationRepository locationRepository,
            GuestSessionRepository sessionRepository,
            IncidentTicketRepository incidentRepository,
            AdministrativeUnitRepository adminUnitRepository,
            GovernmentReportHistoryRepository historyRepository,
            SurveyResponseRepository surveyResponseRepository) {
        this(locationRepository, sessionRepository, incidentRepository, adminUnitRepository, historyRepository, surveyResponseRepository, null, new ObjectMapper());
    }

    @Autowired
    public GovernmentReportService(
            WifiLocationRepository locationRepository,
            GuestSessionRepository sessionRepository,
            IncidentTicketRepository incidentRepository,
            AdministrativeUnitRepository adminUnitRepository,
            GovernmentReportHistoryRepository historyRepository,
            SurveyResponseRepository surveyResponseRepository,
            SurveyQuestionRepository surveyQuestionRepository,
            ObjectMapper objectMapper) {
        this.locationRepository = locationRepository;
        this.sessionRepository = sessionRepository;
        this.incidentRepository = incidentRepository;
        this.adminUnitRepository = adminUnitRepository;
        this.historyRepository = historyRepository;
        this.surveyResponseRepository = surveyResponseRepository;
        this.surveyQuestionRepository = surveyQuestionRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private record PeriodWindow(Instant start, Instant end, String label) {}

    private PeriodWindow resolvePeriodWindow(String period) {
        String code = (period != null && !period.isBlank()) ? period.trim().toUpperCase() : "MONTH";
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        ZonedDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);

        return switch (code) {
            case "MONTH" -> new PeriodWindow(
                    startOfMonth.toInstant(),
                    startOfMonth.plusMonths(1).toInstant(),
                    String.format("Tháng %02d/%d", startOfMonth.getMonthValue(), startOfMonth.getYear())
            );
            case "PREV_MONTH" -> {
                ZonedDateTime prevStart = startOfMonth.minusMonths(1);
                yield new PeriodWindow(
                        prevStart.toInstant(),
                        startOfMonth.toInstant(),
                        String.format("Tháng %02d/%d", prevStart.getMonthValue(), prevStart.getYear())
                );
            }
            case "QUARTER" -> {
                int quarter = ((now.getMonthValue() - 1) / 3) + 1;
                int firstMonth = (quarter - 1) * 3 + 1;
                ZonedDateTime qStart = now.withMonth(firstMonth).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                yield new PeriodWindow(
                        qStart.toInstant(),
                        qStart.plusMonths(3).toInstant(),
                        String.format("Quý %d/%d", quarter, qStart.getYear())
                );
            }
            case "YEAR" -> {
                ZonedDateTime yStart = now.withMonth(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                yield new PeriodWindow(
                        yStart.toInstant(),
                        yStart.plusYears(1).toInstant(),
                        String.format("Năm %d", yStart.getYear())
                );
            }
            default -> new PeriodWindow(
                    Instant.EPOCH,
                    now.plusDays(1).toInstant(),
                    "Toàn thời gian"
            );
        };
    }

    public GovernmentReportDto generateReport(String period) {
        PeriodWindow window = resolvePeriodWindow(period);
        Instant now = Instant.now();

        // 1. Quy hoạch tổng thể đề án 1.584 Nhà văn hóa TP Hải Phòng
        int targetTotalLocations = 1584;

        // 2. Điểm phát sóng thực tế đã triển khai trong hệ thống
        List<WifiLocation> deployedLocations = locationRepository.findAllWithAdminUnitAndDeletedFalse();
        if (deployedLocations == null) {
            deployedLocations = Collections.emptyList();
        }
        int deployedCount = deployedLocations.size();
        Set<Long> onlineDeviceLocationIds = null;
        if (networkDeviceRepository != null) {
            onlineDeviceLocationIds = networkDeviceRepository.findOnlineDeviceLocationIds();
        }
        final Set<Long> liveOnlineIds = onlineDeviceLocationIds;
        int activeLocations = (int) deployedLocations.stream()
                .filter(loc -> loc.isActive() && (liveOnlineIds == null || liveOnlineIds.contains(loc.getId())))
                .count();
        if (deployedCount == 0 && activeLocations == 0) {
            long totalFromCount = locationRepository.countByDeletedFalse();
            long activeFromCount = liveOnlineIds == null ? locationRepository.countByActiveTrueAndDeletedFalse() : 0L;
            deployedCount = (int) totalFromCount;
            activeLocations = (int) activeFromCount;
        }

        double availabilityRate = deployedCount > 0
                ? Math.round((((double) activeLocations / deployedCount) * 100.0) * 10.0) / 10.0
                : 0.0;

        // 3. Số lượt truy cập & dung lượng thực tế trong kỳ báo cáo từ guest_sessions
        long totalSessions = sessionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(window.start(), window.end());
        Long totalBytesSum = sessionRepository.sumTotalBytesBetween(window.start(), window.end());
        long totalBytes = totalBytesSum != null ? totalBytesSum : 0L;
        double dataServedTb = totalBytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);

        // 4. Mức độ hài lòng: tính trực tiếp từ survey_responses trong kỳ
        double satisfactionRate = computeRealSatisfactionRate(window.start(), window.end());

        // 5. Sự cố kỹ thuật, SLA & MTTR thực tế từ incident_tickets trong kỳ
        List<IncidentTicket> periodIncidents = incidentRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(window.start(), window.end());
        if (periodIncidents == null) {
            periodIncidents = Collections.emptyList();
        }
        int totalIncidents = periodIncidents.size();
        long breachedCount = periodIncidents.stream().filter(t -> isIncidentBreached(t, now)).count();
        double slaCompliance = totalIncidents > 0
                ? ((double) (totalIncidents - breachedCount) / totalIncidents) * 100.0
                : (deployedCount > 0 ? 100.0 : 0.0);

        double avgMttrHours = periodIncidents.stream()
                .filter(t -> t.getResolvedAt() != null && t.getCreatedAt() != null && !t.getResolvedAt().isBefore(t.getCreatedAt()))
                .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes() / 60.0)
                .average()
                .orElse(0.0);
        avgMttrHours = Math.round(avgMttrHours * 10.0) / 10.0;

        // 6. Thống kê phiên và dung lượng theo từng Location ID trong kỳ
        Map<Long, Long> locationSessionsMap = new HashMap<>();
        Map<Long, Long> locationBytesMap = new HashMap<>();
        try {
            List<Object[]> sessionStats = sessionRepository.sumSessionStatsGroupByLocationBetween(window.start(), window.end());
            if (sessionStats != null) {
                for (Object[] row : sessionStats) {
                    if (row != null && row.length >= 3 && row[0] != null) {
                        Long locId = ((Number) row[0]).longValue();
                        Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                        Long bytes = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                        locationSessionsMap.put(locId, count);
                        locationBytesMap.put(locId, bytes);
                    }
                }
            }
        } catch (Exception ignored) {}

        // Thống kê sự cố & vi phạm SLA theo Location ID trong kỳ
        Map<Long, Integer> locationIncidentsMap = new HashMap<>();
        Map<Long, Integer> locationBreachedMap = new HashMap<>();
        for (IncidentTicket t : periodIncidents) {
            if (t.getLocationId() != null) {
                locationIncidentsMap.merge(t.getLocationId(), 1, Integer::sum);
                if (isIncidentBreached(t, now)) {
                    locationBreachedMap.merge(t.getLocationId(), 1, Integer::sum);
                }
            }
        }

        // 7. Lấy toàn bộ đơn vị Cấp 2 (114 Phường / Xã / Đặc khu) của TP. Hải Phòng
        List<AdministrativeUnit> units = adminUnitRepository.findByLevelOrderByCodeAsc(2).stream()
                .filter(u -> u != null && (u.getStatus() == null || "ACTIVE".equalsIgnoreCase(u.getStatus())))
                .toList();
        List<DistrictStatDto> unitStats = new ArrayList<>();

        if (units.isEmpty()) {
            unitStats.add(new DistrictStatDto(1L, "Toàn Thành Phố", deployedCount, activeLocations, Math.round(availabilityRate * 10.0) / 10.0, totalSessions, Math.round((totalBytes / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0, totalIncidents, Math.round(slaCompliance * 10.0) / 10.0));
        } else {
            for (AdministrativeUnit u : units) {
                // Lọc các điểm phát trực thuộc Phường / Xã / Đặc khu này
                List<WifiLocation> unitLocs = deployedLocations.stream()
                        .filter(loc -> {
                            if (loc.getAdminUnit() == null) return false;
                            Integer unitLevel = loc.getAdminUnit().getLevel();
                            if (Integer.valueOf(2).equals(unitLevel)) {
                                return Objects.equals(loc.getAdminUnit().getId(), u.getId());
                            }
                            if (Integer.valueOf(3).equals(unitLevel) && loc.getAdminUnit().getParent() != null) {
                                return Objects.equals(loc.getAdminUnit().getParent().getId(), u.getId());
                            }
                            return false;
                        })
                        .toList();

                int unitDeployed = unitLocs.size();
                int unitOnline = (int) unitLocs.stream()
                        .filter(loc -> loc.isActive() && (liveOnlineIds == null || liveOnlineIds.contains(loc.getId())))
                        .count();
                double unitAvailability = unitDeployed > 0
                        ? ((double) unitOnline / unitDeployed) * 100.0
                        : 0.0;

                long unitSessions = 0L;
                long unitBytes = 0L;
                int unitIncidents = 0;
                int unitBreached = 0;

                for (WifiLocation loc : unitLocs) {
                    unitSessions += locationSessionsMap.getOrDefault(loc.getId(), 0L);
                    unitBytes += locationBytesMap.getOrDefault(loc.getId(), 0L);
                    unitIncidents += locationIncidentsMap.getOrDefault(loc.getId(), 0);
                    unitBreached += locationBreachedMap.getOrDefault(loc.getId(), 0);
                }

                double unitDataGb = Math.round((unitBytes / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0;
                double unitSla = unitIncidents > 0
                        ? ((double) (unitIncidents - unitBreached) / unitIncidents) * 100.0
                        : (unitDeployed > 0 ? 100.0 : 0.0);

                unitStats.add(new DistrictStatDto(
                        u.getId(),
                        u.getName(),
                        unitDeployed,
                        unitOnline,
                        Math.round(unitAvailability * 10.0) / 10.0,
                        unitSessions,
                        unitDataGb,
                        unitIncidents,
                        Math.round(unitSla * 10.0) / 10.0
                ));
            }
        }

        return new GovernmentReportDto(
                window.label(),
                "ỦY BAN NHÂN DÂN THÀNH PHỐ HẢI PHÒNG",
                "SỞ KHOA HỌC VÀ CÔNG NGHỆ HẢI PHÒNG",
                Instant.now(),
                targetTotalLocations,
                activeLocations,
                Math.round(availabilityRate * 10.0) / 10.0,
                totalSessions,
                dataServedTb,
                satisfactionRate,
                totalIncidents,
                Math.round(slaCompliance * 10.0) / 10.0,
                avgMttrHours,
                unitStats
        );
    }

    private boolean isIncidentBreached(IncidentTicket ticket, Instant now) {
        if (ticket == null) return false;
        if ("BREACHED".equalsIgnoreCase(ticket.getStatus())) return true;
        if (ticket.getSlaDeadline() == null) return false;
        if (ticket.getResolvedAt() != null) {
            return ticket.getResolvedAt().isAfter(ticket.getSlaDeadline());
        }
        String st = ticket.getStatus();
        boolean stillOpen = !"RESOLVED".equalsIgnoreCase(st) && !"CLOSED".equalsIgnoreCase(st);
        return stillOpen && now.isAfter(ticket.getSlaDeadline());
    }

    private double computeRealSatisfactionRate(Instant start, Instant end) {
        if (surveyResponseRepository == null) return 0.0;
        List<SurveyResponse> responses = surveyResponseRepository.findBySubmittedAtGreaterThanEqualAndSubmittedAtLessThan(start, end);
        if (responses == null || responses.isEmpty()) return 0.0;

        Map<String, String> questionTypes = new HashMap<>();
        if (surveyQuestionRepository != null) {
            for (SurveyQuestion q : surveyQuestionRepository.findAll()) {
                if (q.getId() != null && q.getQuestionType() != null) {
                    questionTypes.put(q.getId().toString(), q.getQuestionType().toUpperCase());
                }
            }
        }

        long totalRated = 0;
        long satisfiedCount = 0;

        for (SurveyResponse resp : responses) {
            if (resp.getAnswersJson() == null || resp.getAnswersJson().isBlank()) continue;
            try {
                Map<String, Object> ansMap = objectMapper.readValue(resp.getAnswersJson(), new TypeReference<Map<String, Object>>() {});
                for (Map.Entry<String, Object> entry : ansMap.entrySet()) {
                    if (entry.getValue() == null) continue;
                    String qType = questionTypes.getOrDefault(entry.getKey(), "");
                    boolean isRatingType = "RATING".equals(qType) || (qType.isEmpty() && entry.getValue() instanceof Number);
                    if (isRatingType) {
                        try {
                            int star = Integer.parseInt(entry.getValue().toString().trim());
                            if (star >= 1 && star <= 5) {
                                totalRated++;
                                if (star >= 4) satisfiedCount++;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }

        if (totalRated == 0) {
            return 0.0;
        }
        return Math.round(((satisfiedCount * 100.0) / totalRated) * 10.0) / 10.0;
    }

    public List<GovernmentReportHistoryDto> getReportHistory() {
        return historyRepository.findAllByOrderBySubmittedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public GovernmentReportHistoryDto submitReport(SubmitReportRequest req, String username) {
        GovernmentReportDto current = generateReport(req.getPeriod());
        String periodLabel = current.reportPeriod();

        String reportCode = "BC-HP-" + System.currentTimeMillis() % 1000000;
        String title = (req.getTitle() != null && !req.getTitle().isBlank())
                ? req.getTitle().trim()
                : "Báo cáo vận hành Wi-Fi 1.584 Nhà văn hóa (" + periodLabel + ")";

        GovernmentReportHistory entity = new GovernmentReportHistory();
        entity.setReportCode(reportCode);
        entity.setTitle(title);
        entity.setPeriod(req.getPeriod().toUpperCase());
        entity.setTotalLocations(current.totalTargetLocations());
        entity.setOnlineLocations(current.activeLocations());
        entity.setSlaPercentage(current.telecomSlaComplianceRate());
        entity.setTotalSessions(current.totalCitizenSessions());
        entity.setTotalDataGb(current.totalDataServedTb() * 1024.0);
        entity.setExportFormat(req.getExportFormat() != null ? req.getExportFormat().toUpperCase() : "PDF");
        entity.setStatus("SUBMITTED");
        entity.setSubmittedBy(username != null ? username : "skhcn@haiphong.gov.vn");
        entity.setSubmittedAt(Instant.now());
        entity.setRecipientAgency(req.getRecipientAgency() != null && !req.getRecipientAgency().isBlank()
                ? req.getRecipientAgency()
                : "UBND TP Hải Phòng & Sở KH&CN");
        entity.setNotes(req.getNotes());

        GovernmentReportHistory saved = historyRepository.save(entity);
        return toDto(saved);
    }

    private GovernmentReportHistoryDto toDto(GovernmentReportHistory e) {
        return new GovernmentReportHistoryDto(
                e.getId(),
                e.getReportCode(),
                e.getTitle(),
                e.getPeriod(),
                e.getTotalLocations(),
                e.getOnlineLocations(),
                e.getSlaPercentage(),
                e.getTotalSessions(),
                e.getTotalDataGb(),
                e.getExportFormat(),
                e.getStatus(),
                e.getSubmittedBy(),
                e.getSubmittedAt(),
                e.getRecipientAgency(),
                e.getNotes(),
                e.getCreatedAt()
        );
    }

    public String exportReportCsv(String period) {
        GovernmentReportDto report = generateReport(period);
        StringBuilder sb = new StringBuilder();
        sb.append("BÁO CÁO KỸ THUẬT & VẬN HÀNH MẠNG WI-FI CÔNG CỘNG\n");
        sb.append("Đơn vị: ").append(report.authorityName()).append(" - ").append(report.cityTitle()).append("\n");
        sb.append("Thời gian xuất báo cáo: ").append(report.generatedAt()).append("\n\n");

        sb.append("CHỈ SỐ TỔNG THỂ TOÀN THÀNH PHỐ:\n");
        sb.append("Tổng điểm phát theo quy hoạch:,").append(report.totalTargetLocations()).append(" điểm NVH\n");
        sb.append("Số điểm phát đang hoạt động:,").append(report.activeLocations()).append(" điểm\n");
        sb.append("Tỷ lệ sẵn sàng mạng (Availability):,").append(report.networkAvailabilityRate()).append("%\n");
        sb.append("Tổng lượt người dân truy cập:,").append(report.totalCitizenSessions()).append(" lượt\n");
        sb.append("Tổng dung lượng dữ liệu phục vụ:,").append(report.totalDataServedTb()).append(" TB\n");
        sb.append("Chỉ số hài lòng của nhân dân:,").append(report.citizenSatisfactionRate()).append("%\n");
        sb.append("Tỷ lệ tuân thủ cam kết SLA viễn thông:,").append(report.telecomSlaComplianceRate()).append("%\n");
        sb.append("Thời gian phục hồi sự cố trung bình (MTTR):,").append(report.averageResolutionHours()).append(" giờ\n\n");

        int unitCount = report.districtBreakdown() != null ? report.districtBreakdown().size() : 0;
        sb.append("CHI TIẾT PHÂN BỔ THEO ").append(unitCount).append(" PHƯỜNG / XÃ / ĐẶC KHU TP HẢI PHÒNG:\n");
        sb.append("STT,Phường / Xã / Đặc khu,Số điểm phát,Online,Tỷ lệ khả dụng (%),Lượt truy cập,Dung lượng (GB),Sự cố,Đạt SLA (%)\n");

        int stt = 1;
        for (DistrictStatDto d : report.districtBreakdown()) {
            sb.append(stt++).append(",")
                    .append(d.districtName()).append(",")
                    .append(d.totalLocations()).append(",")
                    .append(d.onlineLocations()).append(",")
                    .append(d.availabilityPercent()).append("%,")
                    .append(d.totalSessions()).append(",")
                    .append(d.totalDataGb()).append(",")
                    .append(d.totalIncidents()).append(",")
                    .append(d.slaCompliancePercent()).append("%\n");
        }

        return sb.toString();
    }
}
