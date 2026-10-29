package com.wificaptive.reporting;

import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.incidents.repository.IncidentTicketRepository;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.reporting.dto.GovernmentReportDto;
import com.wificaptive.reporting.service.GovernmentReportService;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

    private GovernmentReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new GovernmentReportService(
                locationRepository,
                sessionRepository,
                incidentRepository,
                adminUnitRepository
        );
    }

    @Test
    void testGenerateReport_ReturnsHaiPhongMetrics() {
        when(locationRepository.count()).thenReturn(1584L);
        when(locationRepository.countByActiveTrue()).thenReturn(1580L);
        when(sessionRepository.count()).thenReturn(50000L);
        when(incidentRepository.count()).thenReturn(10L);
        when(incidentRepository.countByStatus("BREACHED")).thenReturn(0L);

        AdministrativeUnit district1 = new AdministrativeUnit();
        district1.setId(1L);
        district1.setName("Quận Hồng Bàng");
        when(adminUnitRepository.findByLevelOrderByCodeAsc(2)).thenReturn(List.of(district1));

        GovernmentReportDto report = reportService.generateReport("MONTH");

        assertNotNull(report);
        assertEquals(1584, report.totalTargetLocations());
        assertEquals(1580, report.activeLocations());
        assertEquals(50000L, report.totalCitizenSessions());
        assertEquals(100.0, report.telecomSlaComplianceRate());
        assertEquals(1, report.districtBreakdown().size());
        assertEquals("Quận Hồng Bàng", report.districtBreakdown().get(0).districtName());

        String csv = reportService.exportReportCsv("MONTH");
        assertTrue(csv.contains("HẢI PHÒNG"));
        assertTrue(csv.contains("Quận Hồng Bàng"));
    }
}
