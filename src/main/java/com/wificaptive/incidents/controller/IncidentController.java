package com.wificaptive.incidents.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.incidents.dto.*;
import com.wificaptive.incidents.service.IncidentTicketService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/incidents")
public class IncidentController {

    private final IncidentTicketService ticketService;

    public IncidentController(IncidentTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ApiResponse<PageResponse<IncidentTicketDto>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.ok(ticketService.getTickets(status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<IncidentTicketDto> getTicketById(@PathVariable Long id) {
        return ApiResponse.ok(ticketService.getTicketById(id));
    }

    @PostMapping
    public ApiResponse<IncidentTicketDto> createTicket(
            @Valid @RequestBody CreateIncidentRequest req,
            Authentication auth) {
        String actor = auth != null ? auth.getName() : "admin";
        return ApiResponse.ok("Tạo phiếu báo sự cố thành công", ticketService.createTicket(req, actor));
    }

    @PostMapping("/{id}/assign")
    public ApiResponse<IncidentTicketDto> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignIncidentRequest req,
            Authentication auth) {
        String actor = auth != null ? auth.getName() : "dispatcher";
        return ApiResponse.ok("Phân công kỹ thuật viên thành công", ticketService.assignTicket(id, req, actor));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<IncidentTicketDto> resolveTicket(
            @PathVariable Long id,
            @Valid @RequestBody ResolveIncidentRequest req,
            Authentication auth) {
        String actor = auth != null ? auth.getName() : "technician";
        return ApiResponse.ok("Xác nhận giải quyết sự cố thành công", ticketService.resolveTicket(id, req, actor));
    }

    @GetMapping("/sla-metrics")
    public ApiResponse<SlaMetricsDto> getSlaMetrics() {
        return ApiResponse.ok(ticketService.calculateSlaMttrMetrics());
    }
}

