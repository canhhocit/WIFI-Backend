package com.wificaptive.locations.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.security.UserPrincipal;
import com.wificaptive.locations.dto.CreateLocationRequest;
import com.wificaptive.locations.dto.LocationDto;
import com.wificaptive.locations.dto.UpdateLocationRequest;
import com.wificaptive.locations.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.wificaptive.locations.dto.excel.ExcelImportSummaryDto;
import com.wificaptive.locations.service.LocationExcelService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/locations")
public class LocationController {

    private final LocationService locationService;
    private final LocationExcelService excelService;

    public LocationController(LocationService locationService, LocationExcelService excelService) {
        this.locationService = locationService;
        this.excelService = excelService;
    }

    @GetMapping
    public ApiResponse<PageResponse<LocationDto>> getLocations(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ApiResponse.ok(locationService.getLocations(query, pageable));
    }

    @GetMapping("/active")
    public ApiResponse<List<LocationDto>> getActiveLocations() {
        return ApiResponse.ok(locationService.getActiveLocations());
    }

    @GetMapping("/{id}")
    public ApiResponse<LocationDto> getLocationById(@PathVariable Long id) {
        return ApiResponse.ok(locationService.getLocationById(id));
    }

    @PostMapping
    public ApiResponse<LocationDto> createLocation(
            @Valid @RequestBody CreateLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LocationDto created = locationService.createLocation(request, principal != null ? principal.getUsername() : "admin");
        return ApiResponse.ok("Location created successfully", created);
    }

    @PutMapping("/{id}")
    public ApiResponse<LocationDto> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LocationDto updated = locationService.updateLocation(id, request, principal != null ? principal.getUsername() : "admin");
        return ApiResponse.ok("Location updated successfully", updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLocation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        locationService.deleteLocation(id, principal != null ? principal.getUsername() : "admin");
        return ApiResponse.ok("Location deleted successfully", null);
    }

    @PostMapping("/import/validate")
    public ApiResponse<ExcelImportSummaryDto> validateExcel(
            @RequestParam("file") MultipartFile file) throws IOException {
        ExcelImportSummaryDto summary = excelService.validateExcel(file.getInputStream());
        return ApiResponse.ok("Kiểm tra file Excel thành công", summary);
    }

    @PostMapping("/import/commit")
    public ApiResponse<Integer> commitExcel(
            @RequestParam("sessionId") String sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = excelService.commitImport(sessionId, principal != null ? principal.getUsername() : "admin");
        return ApiResponse.ok("Đã lưu thành công " + count + " điểm phát sóng vào hệ thống", count);
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = excelService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=1584_Diem_WiFi_HaiPhong_Template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/import/errors/{sessionId}")
    public ResponseEntity<byte[]> downloadErrors(@PathVariable String sessionId) {
        byte[] bytes = excelService.exportErrors(sessionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Danh_sach_dong_loi.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAllLocations() {
        byte[] bytes = excelService.exportAllLocations();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Danh_sach_1584_Diem_WiFi.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
