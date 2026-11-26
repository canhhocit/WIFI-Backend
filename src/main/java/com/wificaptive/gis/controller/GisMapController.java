package com.wificaptive.gis.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.gis.dto.GisLocationPinDto;
import com.wificaptive.incidents.entity.IncidentTicket;
import com.wificaptive.incidents.repository.IncidentTicketRepository;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/gis")
@PreAuthorize("@rbac.hasPermission('LOCATIONS', 'VIEW')")
public class GisMapController {

    private final WifiLocationRepository locationRepository;
    private final GuestSessionRepository sessionRepository;
    private final NetworkDeviceRepository deviceRepository;
    private final IncidentTicketRepository incidentRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wificaptive.geography.service.AdminScopeService adminScopeService;

    public GisMapController(WifiLocationRepository locationRepository,
                             GuestSessionRepository sessionRepository,
                             NetworkDeviceRepository deviceRepository,
                             IncidentTicketRepository incidentRepository) {
        this.locationRepository = locationRepository;
        this.sessionRepository = sessionRepository;
        this.deviceRepository = deviceRepository;
        this.incidentRepository = incidentRepository;
    }

    @GetMapping("/locations")
    @Transactional(readOnly = true)
    public ApiResponse<List<GisLocationPinDto>> getGisLocations() {
        List<WifiLocation> locations;
        try {
            locations = locationRepository.findAllWithAdminUnitAndDeletedFalse();
        } catch (Exception e) {
            locations = locationRepository.findByDeletedFalse(org.springframework.data.domain.Pageable.unpaged()).getContent();
        }

        java.util.Set<Long> scopedUnitIds = adminScopeService != null ? adminScopeService.getCurrentUserScopedUnitIds() : null;
        if (scopedUnitIds != null) {
            locations = locations.stream()
                    .filter(loc -> loc.getAdminUnit() != null && scopedUnitIds.contains(loc.getAdminUnit().getId()))
                    .toList();
        }

        List<GisLocationPinDto> result = new ArrayList<>();

        for (WifiLocation loc : locations) {
            GisLocationPinDto dto = new GisLocationPinDto();
            dto.setId(loc.getId());
            dto.setSlug(loc.getSlug());
            dto.setName(loc.getName());
            dto.setLocationCode(loc.getLocationCode() != null ? loc.getLocationCode() : (loc.getSlug() != null ? loc.getSlug().toUpperCase() : "NVH-" + loc.getId()));
            dto.setAddress(loc.getAddress());

            // Coordinates (default to center of Hai Phong if not set)
            dto.setLatitude(loc.getLatitude() != null ? loc.getLatitude() : 20.8449);
            dto.setLongitude(loc.getLongitude() != null ? loc.getLongitude() : 106.6881);

            // Administrative unit resolution (2-tier: City -> Phường / Xã / Đặc khu)
            if (loc.getAdminUnit() != null) {
                AdministrativeUnit unit = loc.getAdminUnit();
                dto.setAdminUnitId(unit.getId());
                dto.setAdminUnitName(unit.getName());

                String unitLabel = unit.getName();
                if (unit.getLevel() != null && unit.getLevel() == 3 && unit.getParent() != null) {
                    unitLabel = unit.getParent().getName();
                }
                dto.setDistrictName(unitLabel);
            } else {
                dto.setDistrictName(null);
            }

            dto.setIspName(loc.getIspName() != null ? loc.getIspName() : "Viettel");
            dto.setCircuitCode(loc.getCircuitCode());
            dto.setActive(loc.isActive());

            // Network devices count & GWN Router check
            boolean hasGwnRouter = false;
            boolean gwnRouterOnline = false;
            try {
                List<NetworkDevice> devices = deviceRepository.findByLocationId(loc.getId());
                dto.setTotalDevices(devices.size());
                int onlineDevs = 0;
                for (NetworkDevice d : devices) {
                    if ("ONLINE".equalsIgnoreCase(d.getStatus()) || "ACTIVE".equalsIgnoreCase(d.getStatus())) {
                        onlineDevs++;
                    }
                    if ("ROUTER".equalsIgnoreCase(d.getDeviceType())) {
                        hasGwnRouter = true;
                        if ("ONLINE".equalsIgnoreCase(d.getStatus()) || "ACTIVE".equalsIgnoreCase(d.getStatus())) {
                            gwnRouterOnline = true;
                        }
                    }
                }
                dto.setOnlineDevices(onlineDevs);
            } catch (Exception ignored) {
                dto.setTotalDevices(0);
                dto.setOnlineDevices(0);
            }
            dto.setHasGwnRouter(hasGwnRouter);

            // Map status accurately: chỉ ONLINE khi điểm đang bật VÀ có Router GWN đang ONLINE gắn vào
            String status = "ONLINE";
            if (!loc.isActive() || "INACTIVE".equalsIgnoreCase(loc.getStatus())) {
                status = "OFFLINE";
            } else if ("MAINTENANCE".equalsIgnoreCase(loc.getStatus())) {
                status = "DEGRADED";
            } else if (!hasGwnRouter || !gwnRouterOnline) {
                status = "OFFLINE";
            }
            dto.setStatus(status);

            // Active sessions
            long activeSessions = sessionRepository.countByLocationIdAndStatus(loc.getId(), SessionStatus.AUTHORIZED);
            dto.setActiveSessionsCount((int) activeSessions);

            // Incidents count
            try {
                List<IncidentTicket> incidents = incidentRepository.findByLocationId(loc.getId());
                int openIncidents = 0;
                for (IncidentTicket inc : incidents) {
                    if ("OPEN".equalsIgnoreCase(inc.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(inc.getStatus())) {
                        openIncidents++;
                    }
                }
                dto.setOpenIncidentsCount(openIncidents);
            } catch (Exception ignored) {
                dto.setOpenIncidentsCount(0);
            }

            result.add(dto);
        }

        return ApiResponse.ok(result);
    }
}
