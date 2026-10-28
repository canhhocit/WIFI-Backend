package com.wificaptive.devices.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.security.UserPrincipal;
import com.wificaptive.devices.dto.DeviceDto;
import com.wificaptive.devices.service.DeviceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ApiResponse<PageResponse<DeviceDto>> getDevices(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("lastSeenAt").descending());
        return ApiResponse.ok(deviceService.getDevices(search, pageable));
    }

    @PatchMapping("/{id}/block")
    public ApiResponse<DeviceDto> toggleBlockDevice(
            @PathVariable Long id,
            @RequestParam boolean blocked,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeviceDto dto = deviceService.toggleBlockDevice(id, blocked, principal != null ? principal.getUsername() : "admin");
        return ApiResponse.ok(blocked ? "Device blocked" : "Device unblocked", dto);
    }
}
