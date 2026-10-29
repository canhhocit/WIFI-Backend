package com.wificaptive.incidents;

import com.wificaptive.incidents.dto.AssignIncidentRequest;
import com.wificaptive.incidents.dto.CreateIncidentRequest;
import com.wificaptive.incidents.dto.IncidentTicketDto;
import com.wificaptive.incidents.dto.ResolveIncidentRequest;
import com.wificaptive.incidents.dto.SlaMetricsDto;
import com.wificaptive.incidents.entity.IncidentTicket;
import com.wificaptive.incidents.repository.IncidentTicketRepository;
import com.wificaptive.incidents.repository.IncidentUpdateRepository;
import com.wificaptive.incidents.service.IncidentTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    private IncidentTicketService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentTicketService(ticketRepository, updateRepository);
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
    void testCalculateSlaMttrMetrics() {
        IncidentTicket t1 = new IncidentTicket();
        t1.setId(1L);
        t1.setStatus("RESOLVED");
        t1.setCreatedAt(Instant.now().minus(3, ChronoUnit.HOURS));
        t1.setResolvedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        t1.setSlaDeadline(Instant.now().plus(2, ChronoUnit.HOURS)); // On time

        IncidentTicket t2 = new IncidentTicket();
        t2.setId(2L);
        t2.setStatus("RESOLVED");
        t2.setCreatedAt(Instant.now().minus(10, ChronoUnit.HOURS));
        t2.setResolvedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        t2.setSlaDeadline(Instant.now().minus(4, ChronoUnit.HOURS)); // Breached

        when(ticketRepository.findAll()).thenReturn(List.of(t1, t2));

        SlaMetricsDto metrics = incidentService.calculateSlaMttrMetrics();
        assertNotNull(metrics);
        assertEquals(2, metrics.getTotalTickets());
        assertEquals(2, metrics.getResolvedTickets());
        assertEquals(1, metrics.getBreachedTickets());
        assertEquals(50.0, metrics.getSlaCompliancePercentage());
        assertTrue(metrics.getMttrHours() > 0);
    }
}

