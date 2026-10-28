package com.wificaptive.portal.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.portal.dto.GuestLoginRequest;
import com.wificaptive.portal.dto.GuestLoginResponse;
import com.wificaptive.portal.dto.GuestSessionStatusResponse;
import com.wificaptive.portal.dto.PortalInitResponse;
import com.wificaptive.portal.service.PortalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/init")
    public ApiResponse<PortalInitResponse> getPortalInit(
            @RequestParam(required = false) String location,
            HttpServletRequest request) {
        PortalInitResponse response = portalService.getPortalInit(location, request);
        return ApiResponse.ok(response);
    }

    @PostMapping("/login")
    public ApiResponse<GuestLoginResponse> authenticateGuest(
            @Valid @RequestBody GuestLoginRequest request,
            HttpServletRequest httpRequest) {
        GuestLoginResponse response = portalService.authenticateGuest(request, httpRequest);
        return ApiResponse.ok(response.getMessage(), response);
    }

    @GetMapping("/session/{token}")
    public ApiResponse<GuestSessionStatusResponse> getSessionStatus(@PathVariable String token) {
        GuestSessionStatusResponse response = portalService.getGuestSessionStatus(token);
        return ApiResponse.ok(response);
    }

    @PostMapping("/session/{token}/disconnect")
    public ApiResponse<GuestSessionStatusResponse> disconnectSession(@PathVariable String token) {
        GuestSessionStatusResponse response = portalService.disconnectGuestSession(token);
        return ApiResponse.ok("Session disconnected", response);
    }
}
