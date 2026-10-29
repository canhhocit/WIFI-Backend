package com.wificaptive.gis.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.gis.dto.GisLocationPinDto;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/gis")
public class GisMapController {

    private final WifiLocationRepository locationRepository;
    private final GuestSessionRepository sessionRepository;

    public GisMapController(WifiLocationRepository locationRepository, GuestSessionRepository sessionRepository) {
        this.locationRepository = locationRepository;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/locations")
    public ApiResponse<List<GisLocationPinDto>> getGisLocations() {
        List<WifiLocation> locations = locationRepository.findAll();
        List<GisLocationPinDto> result = new ArrayList<>();

        for (WifiLocation loc : locations) {
            GisLocationPinDto dto = new GisLocationPinDto();
            dto.setId(loc.getId());
            dto.setSlug(loc.getSlug());
            dto.setName(loc.getName());
            dto.setAddress(loc.getAddress());

            // Coordinates (default to center of Hai Phong if not set)
            dto.setLatitude(loc.getLatitude() != null ? loc.getLatitude() : 20.8449);
            dto.setLongitude(loc.getLongitude() != null ? loc.getLongitude() : 106.6881);

            dto.setAdminUnitName(loc.getAdminUnit() != null ? loc.getAdminUnit().getName() : null);
            dto.setIspName(loc.getIspName() != null ? loc.getIspName() : "VNPT");
            dto.setCircuitCode(loc.getCircuitCode());
            dto.setStatus(loc.isActive() ? "ONLINE" : "OFFLINE");

            // Count active sessions
            long activeSessions = sessionRepository.countByLocationIdAndStatus(loc.getId(), SessionStatus.AUTHORIZED);
            dto.setActiveSessionsCount((int) activeSessions);

            result.add(dto);
        }

        return ApiResponse.ok(result);
    }
}

