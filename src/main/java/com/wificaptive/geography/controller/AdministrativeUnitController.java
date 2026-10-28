package com.wificaptive.geography.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.geography.dto.AdminUnitCreateRequest;
import com.wificaptive.geography.dto.AdminUnitDto;
import com.wificaptive.geography.dto.MergeUnitsRequest;
import com.wificaptive.geography.service.AdministrativeUnitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/geography")
public class AdministrativeUnitController {

    private final AdministrativeUnitService unitService;

    public AdministrativeUnitController(AdministrativeUnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<AdminUnitDto>>> getTree() {
        return ResponseEntity.ok(ApiResponse.ok(unitService.getAllTree()));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<ApiResponse<List<AdminUnitDto>>> getByLevel(@PathVariable Integer level) {
        return ResponseEntity.ok(ApiResponse.ok(unitService.getByLevel(level)));
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<ApiResponse<List<AdminUnitDto>>> getChildren(@PathVariable Long parentId) {
        return ResponseEntity.ok(ApiResponse.ok(unitService.getChildren(parentId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUnitDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(unitService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUnitDto>> createUnit(@Valid @RequestBody AdminUnitCreateRequest request) {
        AdminUnitDto dto = unitService.createUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Tạo đơn vị địa bàn thành công", dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUnitDto>> updateUnit(@PathVariable Long id, @Valid @RequestBody AdminUnitCreateRequest request) {
        AdminUnitDto dto = unitService.updateUnit(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật đơn vị địa bàn thành công", dto));
    }

    @PostMapping("/merge")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> mergeUnits(@Valid @RequestBody MergeUnitsRequest request) {
        unitService.mergeUnits(request);
        return ResponseEntity.ok(ApiResponse.ok("Sáp nhập đơn vị địa bàn thành công", null));
    }
}
