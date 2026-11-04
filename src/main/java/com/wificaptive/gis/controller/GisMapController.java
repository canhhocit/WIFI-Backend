package com.wificaptive.gis.controller;

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
public class GisMapController {

    private final WifiLocationRepository locationRepository;
    private final GuestSessionRepository sessionRepository;
    private final NetworkDeviceRepository deviceRepository;
    private final IncidentTicketRepository incidentRepository;

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
            locations = locationRepository.findAllWithAdminUnit();
        } catch (Exception e) {
            locations = locationRepository.findAll();
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

            // Administrative unit and district resolution
            if (loc.getAdminUnit() != null) {
                AdministrativeUnit unit = loc.getAdminUnit();
                dto.setAdminUnitId(unit.getId());
                dto.setAdminUnitName(unit.getName());

                String district = null;
                if (unit.getLevel() != null && unit.getLevel() == 2) {
                    district = unit.getName();
                } else if (unit.getParent() != null) {
                    district = unit.getParent().getName();
                } else {
                    district = inferDistrictFromText(loc.getAddress(), loc.getName());
                }
                dto.setDistrictName(district);
            } else {
                dto.setDistrictName(inferDistrictFromText(loc.getAddress(), loc.getName()));
            }

            dto.setIspName(loc.getIspName() != null ? loc.getIspName() : "Viettel");
            dto.setCircuitCode(loc.getCircuitCode());
            dto.setActive(loc.isActive());

            // Map status accurately
            String status = "ONLINE";
            if (!loc.isActive() || "INACTIVE".equalsIgnoreCase(loc.getStatus())) {
                status = "OFFLINE";
            } else if ("MAINTENANCE".equalsIgnoreCase(loc.getStatus())) {
                status = "DEGRADED";
            }
            dto.setStatus(status);

            // Active sessions
            long activeSessions = sessionRepository.countByLocationIdAndStatus(loc.getId(), SessionStatus.AUTHORIZED);
            dto.setActiveSessionsCount((int) activeSessions);

            // Network devices count
            try {
                List<NetworkDevice> devices = deviceRepository.findByLocationId(loc.getId());
                dto.setTotalDevices(devices.size());
                int onlineDevs = 0;
                for (NetworkDevice d : devices) {
                    if ("ONLINE".equalsIgnoreCase(d.getStatus()) || "ACTIVE".equalsIgnoreCase(d.getStatus())) {
                        onlineDevs++;
                    }
                }
                dto.setOnlineDevices(onlineDevs);
            } catch (Exception ignored) {
                dto.setTotalDevices(0);
                dto.setOnlineDevices(0);
            }

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

    private String inferDistrictFromText(String address, String name) {
        String combined = ((address != null ? address : "") + " " + (name != null ? name : "")).toLowerCase();
        if (combined.contains("thủy nguyên") || combined.contains("thuy nguyen") || combined.contains("núi đèo") || combined.contains("nui deo")) return "Huyện Thủy Nguyên";
        if (combined.contains("hồng bàng") || combined.contains("hong bang") || combined.contains("hoàng văn thụ") || combined.contains("hoang van thu")) return "Quận Hồng Bàng";
        if (combined.contains("ngô quyền") || combined.contains("ngo quyen") || combined.contains("máy tơ") || combined.contains("may to")) return "Quận Ngô Quyền";
        if (combined.contains("lê chân") || combined.contains("le chan") || combined.contains("an biên") || combined.contains("an bien")) return "Quận Lê Chân";
        if (combined.contains("hải an") || combined.contains("hai an")) return "Quận Hải An";
        if (combined.contains("kiến an") || combined.contains("kien an")) return "Quận Kiến An";
        if (combined.contains("đồ sơn") || combined.contains("do son")) return "Quận Đồ Sơn";
        if (combined.contains("dương kinh") || combined.contains("duong kinh")) return "Quận Dương Kinh";
        if (combined.contains("an dương") || combined.contains("an duong")) return "Huyện An Dương";
        if (combined.contains("an lão") || combined.contains("an lao")) return "Huyện An Lão";
        if (combined.contains("kiến thụy") || combined.contains("kien thuy")) return "Huyện Kiến Thụy";
        if (combined.contains("tiên lãng") || combined.contains("tien lang")) return "Huyện Tiên Lãng";
        if (combined.contains("vĩnh bảo") || combined.contains("vinh bao")) return "Huyện Vĩnh Bảo";
        if (combined.contains("cát hải") || combined.contains("cat hai") || combined.contains("cát bà") || combined.contains("cat ba")) return "Huyện Cát Hải";
        if (combined.contains("bạch long vĩ") || combined.contains("bach long vi")) return "Huyện Bạch Long Vĩ";
        return null;
    }
}
