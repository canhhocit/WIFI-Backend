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

import java.util.List;

@RestController
@RequestMapping("/api/admin/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
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
}
