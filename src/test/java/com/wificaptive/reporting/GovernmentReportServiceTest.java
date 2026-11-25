package com.wificaptive.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.incidents.entity.IncidentTicket;
import com.wificaptive.incidents.repository.IncidentTicketRepository;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.reporting.dto.GovernmentReportDto;
import com.wificaptive.reporting.repository.GovernmentReportHistoryRepository;
import com.wificaptive.reporting.service.GovernmentReportService;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import com.wificaptive.surveys.entity.SurveyQuestion;
import com.wificaptive.surveys.entity.SurveyResponse;
import com.wificaptive.surveys.repository.SurveyQuestionRepository;
import com.wificaptive.surveys.repository.SurveyResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GovernmentReportServiceTest {

    @Mock
    private WifiLocationRepository locationRepository;
    @Mock
    private GuestSessionRepository sessionRepository;
    @Mock
    private IncidentTicketRepository incidentRepository;
    @Mock
    private AdministrativeUnitRepository adminUnitRepository;
    @Mock
    private GovernmentReportHistoryRepository historyRepository;
    @Mock
    private SurveyResponseRepository surveyResponseRepository;
    @Mock
    private SurveyQuestionRepository surveyQuestionRepository;

    private GovernmentReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new GovernmentReportService(
                locationRepository,
                sessionRepository,
                incidentRepository,
                adminUnitRepository,
                historyRepository,
                surveyResponseRepository,
                surveyQuestionRepository,
                new ObjectMapper()
        );
    }

    @Test
    void testGenerateReport_ComputesRealMetricsWithoutHardcodedValues() {
        AdministrativeUnit districtHb = new AdministrativeUnit();
        districtHb.setId(1L);
        districtHb.setCode("HP_HB");
        districtHb.setName("Quận Hồng Bàng");
        districtHb.setLevel(2);

        AdministrativeUnit districtAd = new AdministrativeUnit();
        districtAd.setId(2L);
        districtAd.setCode("HP_AD");
        districtAd.setName("Huyện An Dương");
        districtAd.setLevel(2);

        WifiLocation loc1 = new WifiLocation();
        loc1.setId(10L);
        loc1.setAdminUnit(districtHb);
        loc1.setActive(true);

        when(locationRepository.findAllWithAdminUnitAndDeletedFalse()).thenReturn(List.of(loc1));
        when(sessionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(73L);
        when(sessionRepository.sumTotalBytesBetween(any(), any())).thenReturn(52L * 1024 * 1024); // 52 MB
        when(sessionRepository.sumSessionStatsGroupByLocationBetween(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 73L, 52L * 1024 * 1024}));

        Instant tStart = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant tResolved = Instant.now().minus(1, ChronoUnit.HOURS); // 2.0h MTTR
        IncidentTicket ticket = new IncidentTicket();
        ticket.setLocationId(10L);
        ticket.setStatus("RESOLVED");
        ticket.setCreatedAt(tStart);
        ticket.setResolvedAt(tResolved);
        ticket.setSlaDeadline(tStart.plus(4, ChronoUnit.HOURS));
        when(incidentRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(List.of(ticket));

        SurveyQuestion rq = new SurveyQuestion();
        rq.setId(100L);
        rq.setQuestionType("RATING");
        when(surveyQuestionRepository.findAll()).thenReturn(List.of(rq));

        SurveyResponse r1 = new SurveyResponse();
        r1.setAnswersJson("{\"100\":5}");
        SurveyResponse r2 = new SurveyResponse();
        r2.setAnswersJson("{\"100\":2}");
        when(surveyResponseRepository.findBySubmittedAtGreaterThanEqualAndSubmittedAtLessThan(any(), any()))
                .thenReturn(List.of(r1, r2));

        when(adminUnitRepository.findByLevelOrderByCodeAsc(2)).thenReturn(List.of(districtHb, districtAd));

        GovernmentReportDto report = reportService.generateReport("MONTH");

        assertNotNull(report);
        assertEquals("SỞ KHOA HỌC VÀ CÔNG NGHỆ HẢI PHÒNG", report.authorityName());
        assertTrue(report.reportPeriod().startsWith("Tháng "), "Should format period label in Vietnamese");
        assertEquals(1584, report.totalTargetLocations());
        assertEquals(1, report.activeLocations());
        assertEquals(100.0, report.networkAvailabilityRate());
        assertEquals(73L, report.totalCitizenSessions());
        assertEquals(50.0, report.citizenSatisfactionRate()); // 1 out of 2 >= 4 stars
        assertEquals(1, report.totalIncidentsReported());
        assertEquals(100.0, report.telecomSlaComplianceRate());
        assertEquals(2.0, report.averageResolutionHours());

        assertEquals(2, report.districtBreakdown().size());
        assertEquals(100.0, report.districtBreakdown().get(0).availabilityPercent());
        assertEquals(0.0, report.districtBreakdown().get(1).availabilityPercent()); // An Duong has 0 deployed
        assertEquals(0.0, report.districtBreakdown().get(1).slaCompliancePercent());

        String csv = reportService.exportReportCsv("MONTH");
        assertTrue(csv.contains("SỞ KHOA HỌC VÀ CÔNG NGHỆ HẢI PHÒNG"));
        assertTrue(csv.contains("Quận Hồng Bàng"));
    }
}
