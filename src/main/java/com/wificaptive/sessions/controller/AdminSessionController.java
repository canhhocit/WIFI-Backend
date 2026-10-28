package com.wificaptive.sessions.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.security.UserPrincipal;
import com.wificaptive.sessions.dto.GuestSessionDto;
import com.wificaptive.sessions.dto.RevokeSessionRequest;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.service.GuestSessionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sessions")
public class AdminSessionController {

    private final GuestSessionService sessionService;

    public AdminSessionController(GuestSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ApiResponse<PageResponse<GuestSessionDto>> getSessions(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        PageResponse<GuestSessionDto> response = sessionService.getSessions(locationId, status, search, pageable);
        return ApiResponse.ok(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<GuestSessionDto> getSessionById(@PathVariable Long id) {
        return ApiResponse.ok(sessionService.getSessionById(id));
    }

    @PostMapping("/{id}/revoke")
    public ApiResponse<GuestSessionDto> revokeSession(
            @PathVariable Long id,
            @Valid @RequestBody RevokeSessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String actor = principal != null ? principal.getUsername() : "admin";
        GuestSessionDto revoked = sessionService.revokeSession(id, request.getReason(), actor);
        return ApiResponse.ok("Session successfully revoked", revoked);
    }
}
