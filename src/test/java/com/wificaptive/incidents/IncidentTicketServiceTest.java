package com.wificaptive.incidents;

import com.wificaptive.incidents.dto.AssignIncidentRequest;
import com.wificaptive.incidents.dto.CreateIncidentRequest;
import com.wificaptive.incidents.dto.IncidentTicketDto;
import com.wificaptive.incidents.dto.ResolveIncidentRequest;
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
}

