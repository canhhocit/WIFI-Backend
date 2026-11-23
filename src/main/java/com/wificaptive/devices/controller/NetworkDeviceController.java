package com.wificaptive.devices.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.devices.dto.DeviceSwapRequest;
import com.wificaptive.devices.dto.DeviceSyncSummaryDto;
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
    private final com.wificaptive.devices.service.NetworkDeviceImportService importService;

    public NetworkDeviceController(NetworkDeviceService deviceService,
                                   com.wificaptive.devices.service.NetworkDeviceImportService importService) {
        this.deviceService = deviceService;
        this.importService = importService;
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

    @PostMapping("/sync-grandstream")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DeviceSyncSummaryDto>> syncFromGrandstream(Authentication auth) {
        DeviceSyncSummaryDto summary = deviceService.syncFromGrandstream(auth != null ? auth.getName() : "admin");
        return ResponseEntity.ok(ApiResponse.ok(summary.getMessage(), summary));
    }

    @PostMapping("/{id}/reboot")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<NetworkDeviceDto>> rebootDevice(
            @PathVariable Long id,
            Authentication auth) {
        NetworkDeviceDto dto = deviceService.rebootDevice(id, auth != null ? auth.getName() : "admin");
        return ResponseEntity.ok(ApiResponse.ok("Lệnh khởi động lại thiết bị đã được phát thành công", dto));
    }

    @GetMapping("/{id}/clients")
    public ResponseEntity<ApiResponse<List<com.wificaptive.gateway.dto.GwnClientDto>>> getConnectedClients(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getConnectedClients(id)));
    }

    
    /** Import danh sách thiết bị bàn giao (MAC + S/N ...). dryRun=true chỉ kiểm tra, không ghi. */
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.wificaptive.devices.dto.NetworkDeviceImportResultDto>> importDevices(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(defaultValue = "true") boolean dryRun,
            Authentication auth) throws java.io.IOException {
        var result = importService.importFile(file.getInputStream(), file.getOriginalFilename(), dryRun,
                auth != null ? auth.getName() : "admin");
        String msg = dryRun
                ? String.format("Kiểm tra xong: %d dòng hợp lệ để thêm/cập nhật, %d lỗi", result.created() + result.updated(), result.errors().size())
                : String.format("Đã import: thêm %d, cập nhật %d, lỗi %d", result.created(), result.updated(), result.errors().size());
        return ResponseEntity.ok(ApiResponse.ok(msg, result));
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() {
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Danh_sach_thiet_bi_ban_giao.xlsx")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(importService.generateTemplate());
    }

    @GetMapping("/{id}/mesh")
    public ResponseEntity<ApiResponse<com.wificaptive.devices.mesh.MeshApSnapshotDto>> getMeshSnapshot(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean refresh) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getMeshSnapshot(id, refresh)));
    }

    @PostMapping("/upgrade-firmware")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.wificaptive.gateway.dto.GwnUpgradeResult>> upgradeFirmware(
            @RequestBody UpgradeFirmwareRequest req,
            Authentication auth) {
        com.wificaptive.gateway.dto.GwnUpgradeResult res = deviceService.upgradeFirmware(req.deviceIds(), req.targetFirmware(), auth != null ? auth.getName() : "admin");
        return ResponseEntity.ok(ApiResponse.ok(res.message(), res));
    }

    public record UpgradeFirmwareRequest(List<Long> deviceIds, String targetFirmware) {}
}
