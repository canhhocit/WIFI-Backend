package com.wificaptive.devices.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.devices.dto.DeviceSwapRequest;
import com.wificaptive.devices.dto.NetworkDeviceDto;
import com.wificaptive.devices.service.NetworkDeviceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/network-devices")
public class NetworkDeviceController {

    private final NetworkDeviceService deviceService;

    public NetworkDeviceController(NetworkDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NetworkDeviceDto>>> getDevices(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long adminUnitId,
            @PageableDefault(size = 20, sort = "lastSeenAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDevices(type, status, adminUnitId, pageable)));
    }

    @GetMapping("/location/{locationId}")
    public ResponseEntity<ApiResponse<List<NetworkDeviceDto>>> getByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDevicesByLocation(locationId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NetworkDeviceDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDeviceById(id)));
    }

    @PostMapping("/swap")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<NetworkDeviceDto>> swapDevice(
            @Valid @RequestBody DeviceSwapRequest request,
            Authentication auth) {
        NetworkDeviceDto dto = deviceService.swapDevice(request, auth != null ? auth.getName() : "admin");
        return ResponseEntity.ok(ApiResponse.ok("Hoán đổi thiết bị thành công", dto));
    }
}
