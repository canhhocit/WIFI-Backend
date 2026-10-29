package com.wificaptive.incidents.service;

import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.incidents.dto.*;
import com.wificaptive.incidents.entity.IncidentTicket;
import com.wificaptive.incidents.entity.IncidentUpdate;
import com.wificaptive.incidents.repository.IncidentTicketRepository;
import com.wificaptive.incidents.repository.IncidentUpdateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IncidentTicketService {

    private static final Logger log = LoggerFactory.getLogger(IncidentTicketService.class);
    private static final AtomicInteger COUNTER = new AtomicInteger(100);

    private final IncidentTicketRepository ticketRepository;
    private final IncidentUpdateRepository updateRepository;

    public IncidentTicketService(IncidentTicketRepository ticketRepository, IncidentUpdateRepository updateRepository) {
        this.ticketRepository = ticketRepository;
        this.updateRepository = updateRepository;
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

    @Transactional(readOnly = true)
    public SlaMetricsDto calculateSlaMttrMetrics() {
        List<IncidentTicket> all = ticketRepository.findAll();
        long total = all.size();
        long open = all.stream().filter(t -> !"RESOLVED".equalsIgnoreCase(t.getStatus()) && !"CLOSED".equalsIgnoreCase(t.getStatus())).count();
        long resolved = all.stream().filter(t -> "RESOLVED".equalsIgnoreCase(t.getStatus()) || "CLOSED".equalsIgnoreCase(t.getStatus())).count();

        long breached = all.stream().filter(t -> {
            if (t.getResolvedAt() != null) {
                return t.getResolvedAt().isAfter(t.getSlaDeadline());
            } else {
                return Instant.now().isAfter(t.getSlaDeadline());
            }
        }).count();

        double compliance = total > 0 ? ((double) (total - breached) / total) * 100.0 : 100.0;

        // Calculate MTTR in hours for resolved tickets
        double totalResolutionHours = 0;
        int resolvedWithDuration = 0;
        for (IncidentTicket t : all) {
            if (t.getResolvedAt() != null && t.getCreatedAt() != null) {
                long durationMins = Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes();
                totalResolutionHours += (durationMins / 60.0);
                resolvedWithDuration++;
            }
        }

        double mttr = resolvedWithDuration > 0 ? (totalResolutionHours / resolvedWithDuration) : 0.0;

        SlaMetricsDto dto = new SlaMetricsDto();
        dto.setTotalTickets(total);
        dto.setOpenTickets(open);
        dto.setResolvedTickets(resolved);
        dto.setBreachedTickets(breached);
        dto.setSlaCompliancePercentage(Math.round(compliance * 10.0) / 10.0);
        dto.setMttrHours(Math.round(mttr * 10.0) / 10.0);
        return dto;
    }
}

