package com.wificaptive.incidents;

import com.wificaptive.incidents.dto.*;
import com.wificaptive.incidents.entity.CitizenIssueReport;
import com.wificaptive.incidents.entity.IncidentTicket;
import com.wificaptive.incidents.repository.CitizenIssueReportRepository;
import com.wificaptive.incidents.repository.IncidentTicketRepository;
import com.wificaptive.incidents.repository.IncidentUpdateRepository;
import com.wificaptive.incidents.service.IncidentTicketService;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentTicketServiceTest {

    @Mock
    private IncidentTicketRepository ticketRepository;

    @Mock
    private IncidentUpdateRepository updateRepository;

    @Mock
    private CitizenIssueReportRepository citizenReportRepository;

    @Mock
    private WifiLocationRepository locationRepository;

    @Mock
    private com.wificaptive.devices.repository.NetworkDeviceRepository networkDeviceRepository;

    private IncidentTicketService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentTicketService(
                ticketRepository,
                updateRepository,
                citizenReportRepository,
                locationRepository,
                networkDeviceRepository
        );
    }

    @Test
    void testCreateAssignAndResolveIncident() {
        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setLocationId(1L);
        req.setIncidentType("FIBER_CUT");
        req.setSeverity("CRITICAL");
        req.setTitle("Đứt cáp quang tuyến An Dương");
        req.setSlaHours(4);

        IncidentTicket t = new IncidentTicket();
        t.setId(101L);
        t.setTicketCode("SC-20261028-0101");
        t.setLocationId(1L);
        t.setSeverity("CRITICAL");
        t.setStatus("NEW");
        t.setSlaDeadline(Instant.now().plus(4, ChronoUnit.HOURS));

        when(ticketRepository.save(any(IncidentTicket.class))).thenReturn(t);
        when(ticketRepository.findById(101L)).thenReturn(Optional.of(t));

        IncidentTicketDto created = incidentService.createTicket(req, "system_alert");
        assertNotNull(created);
        assertEquals("NEW", created.getStatus());

        // Assign
        AssignIncidentRequest assignReq = new AssignIncidentRequest();
        assignReq.setAssignedTo("VNPT_Engineer_01");
        IncidentTicketDto assigned = incidentService.assignTicket(101L, assignReq, "lead_dispatcher");
        assertEquals("ASSIGNED", assigned.getStatus());
        assertEquals("VNPT_Engineer_01", assigned.getAssignedTo());

        // Resolve
        ResolveIncidentRequest resolveReq = new ResolveIncidentRequest();
        resolveReq.setResolutionNotes("Hàn nối lại sợi quang suy hao 0.02dB");
        IncidentTicketDto resolved = incidentService.resolveTicket(101L, resolveReq, "VNPT_Engineer_01");
        assertEquals("RESOLVED", resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
    }

    @Test
    void testRecordCitizenReport_AutoCreatesIncidentOn3ReportsIn30Mins() {
        WifiLocation loc = new WifiLocation();
        loc.setId(5L);
        loc.setName("Nhà văn hóa Thôn 5, Thủy Nguyên");
        loc.setSlug("hp-tn-nvh05");

        when(locationRepository.findById(5L)).thenReturn(Optional.of(loc));

        CitizenIssueReport rep1 = new CitizenIssueReport();
        rep1.setId(1L);
        rep1.setLocationId(5L);
        rep1.setIssueType("SLOW_CONNECTION");

        CitizenIssueReport rep2 = new CitizenIssueReport();
        rep2.setId(2L);
        rep2.setLocationId(5L);
        rep2.setIssueType("SLOW_CONNECTION");

        CitizenIssueReport rep3 = new CitizenIssueReport();
        rep3.setId(3L);
        rep3.setLocationId(5L);
        rep3.setIssueType("CANNOT_CONNECT");

        List<CitizenIssueReport> pending = new ArrayList<>(List.of(rep1, rep2, rep3));

        when(citizenReportRepository.save(any(CitizenIssueReport.class))).thenAnswer(invocation -> {
            CitizenIssueReport arg = invocation.getArgument(0);
            if (arg.getId() == null) arg.setId(3L);
            return arg;
        });

        when(citizenReportRepository.findByLocationIdAndCreatedAtAfterAndIncidentTicketIdIsNullOrderByCreatedAtAsc(eq(5L), any(Instant.class)))
                .thenReturn(pending);

        when(ticketRepository.findByLocationIdAndStatusIn(eq(5L), anyList()))
                .thenReturn(Collections.emptyList());

        IncidentTicket autoTicket = new IncidentTicket();
        autoTicket.setId(201L);
        autoTicket.setTicketCode("SC-20261113-0201");
        autoTicket.setLocationId(5L);
        autoTicket.setStatus("NEW");

        when(ticketRepository.save(any(IncidentTicket.class))).thenReturn(autoTicket);

        CreateCitizenReportRequest req = new CreateCitizenReportRequest();
        req.setLocationId(5L);
        req.setIssueType("CANNOT_CONNECT");
        req.setDescription("Mạng không load được trang web");
        req.setPhoneNumber("0988123456");

        CitizenReportResponse res = incidentService.recordCitizenReport(req, "192.168.1.100");

        assertNotNull(res);
        assertTrue(res.isIncidentCreated(), "Should trigger automatic incident creation when >= 3 reports in 30 mins");
        assertEquals("SC-20261113-0201", res.getTicketCode());
        assertEquals("Nhà văn hóa Thôn 5, Thủy Nguyên", res.getLocationName());
        verify(ticketRepository, times(1)).save(any(IncidentTicket.class));
    }
}

