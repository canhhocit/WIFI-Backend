package com.wificaptive.incidents.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.incidents.dto.*;
import com.wificaptive.incidents.service.IncidentTicketService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

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
    @PreAuthorize("@rbac.hasPermission('INCIDENTS', 'CREATE')")
    public ApiResponse<IncidentTicketDto> createTicket(
            @Valid @RequestBody CreateIncidentRequest req,
            Authentication auth) {
        String actor = auth != null ? auth.getName() : "admin";
        return ApiResponse.ok("Tạo phiếu báo sự cố thành công", ticketService.createTicket(req, actor));
    }

    @RequestMapping(value = "/{id}/assign", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("@rbac.hasPermission('INCIDENTS', 'EDIT') or @rbac.hasPermission('INCIDENTS', 'APPROVE')")
    public ApiResponse<IncidentTicketDto> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignIncidentRequest req,
            Authentication auth) {
        String actor = auth != null ? auth.getName() : "dispatcher";
        return ApiResponse.ok("Phân công kỹ thuật viên thành công", ticketService.assignTicket(id, req, actor));
    }

    @RequestMapping(value = "/{id}/resolve", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("@rbac.hasPermission('INCIDENTS', 'EDIT') or @rbac.hasPermission('INCIDENTS', 'APPROVE')")
    public ApiResponse<IncidentTicketDto> resolveTicket(
            @PathVariable Long id,
            @Valid @RequestBody ResolveIncidentRequest req,
            Authentication auth) {
        String actor = auth != null ? auth.getName() : "technician";
        return ApiResponse.ok("Xác nhận giải quyết sự cố thành công", ticketService.resolveTicket(id, req, actor));
    }

    @RequestMapping(value = "/{id}/close", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("@rbac.hasPermission('INCIDENTS', 'APPROVE') or @rbac.hasPermission('INCIDENTS', 'EDIT')")
    public ApiResponse<IncidentTicketDto> closeTicket(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String note = body != null ? body.get("note") : null;
        String actor = auth != null ? auth.getName() : "admin";
        return ApiResponse.ok("Xác nhận nghiệm thu và hoàn tất đóng phiếu sự cố", ticketService.closeTicket(id, note, actor));
    }

    @RequestMapping(value = "/{id}/reopen", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("@rbac.hasPermission('INCIDENTS', 'EDIT') or @rbac.hasPermission('INCIDENTS', 'APPROVE')")
    public ApiResponse<IncidentTicketDto> reopenTicket(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String reason = body != null ? body.get("reason") : null;
        String actor = auth != null ? auth.getName() : "admin";
        return ApiResponse.ok("Đã mở lại phiếu sự cố để tiếp tục xử lý", ticketService.reopenTicket(id, reason, actor));
    }

    @GetMapping("/citizen-reports")
    public ApiResponse<PageResponse<CitizenReportDto>> getCitizenReports(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.ok(ticketService.getCitizenReports(locationId, status, page, size));
    }
}

