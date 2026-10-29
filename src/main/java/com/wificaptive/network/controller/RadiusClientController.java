package com.wificaptive.network.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.network.dto.CreateRadiusClientRequest;
import com.wificaptive.network.dto.RadiusClientDto;
import com.wificaptive.network.service.RadiusClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/radius-clients")
@PreAuthorize("hasRole('ADMIN')")
public class RadiusClientController {

    private final RadiusClientService radiusClientService;

    public RadiusClientController(RadiusClientService radiusClientService) {
        this.radiusClientService = radiusClientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RadiusClientDto>>> getAllClients() {
        return ResponseEntity.ok(ApiResponse.ok(radiusClientService.getAllClients()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RadiusClientDto>> createClient(
            @Valid @RequestBody CreateRadiusClientRequest req
    ) {
        RadiusClientDto created = radiusClientService.createClient(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<RadiusClientDto>> toggleClient(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(radiusClientService.toggleClient(id)));
    }
}
