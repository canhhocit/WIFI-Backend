package com.wificaptive.locations.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.locations.dto.CreateLocationRequest;
import com.wificaptive.locations.dto.LocationDto;
import com.wificaptive.locations.dto.UpdateLocationRequest;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocationService {

    private final WifiLocationRepository locationRepository;
    private final AdministrativeUnitRepository adminUnitRepository;
    private final AuditService auditService;
    private final GuestSessionRepository sessionRepository;

    public LocationService(WifiLocationRepository locationRepository,
            AdministrativeUnitRepository adminUnitRepository,
            AuditService auditService) {
        this(locationRepository, adminUnitRepository, auditService, null);
    }

    @Autowired
    public LocationService(WifiLocationRepository locationRepository,
            AdministrativeUnitRepository adminUnitRepository,
            AuditService auditService,
            @Autowired(required = false) GuestSessionRepository sessionRepository) {
        this.locationRepository = locationRepository;
        this.adminUnitRepository = adminUnitRepository;
        this.auditService = auditService;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<LocationDto> getLocations(String query, Pageable pageable) {
        Page<WifiLocation> page;
        if (StringUtils.hasText(query)) {
            page = locationRepository.findByNameContainingIgnoreCaseAndDeletedFalse(query.trim(), pageable);
        } else {
            page = locationRepository.findByDeletedFalse(pageable);
        }

        Map<Long, Long> activeMap = new HashMap<>();
        if (sessionRepository != null) {
            List<Object[]> groups = sessionRepository.countActiveSessionsGroupByLocation(Instant.now());
            for (Object[] g : groups) {
                if (g[0] != null && g[1] != null) {
                    activeMap.put(((Number) g[0]).longValue(), ((Number) g[1]).longValue());
                }
            }
        }

        Page<LocationDto> dtoPage = page.map(loc -> {
            LocationDto dto = LocationDto.from(loc);
            long active = activeMap.getOrDefault(loc.getId(), 0L);
            dto.setActiveSessionsCount(active);
            int maxUsers = loc.getMaxConcurrentUsers() != null ? loc.getMaxConcurrentUsers() : 50;
            dto.setCapacityUsagePercent(maxUsers > 0 ? Math.min(100.0, Math.round((active * 100.0 / maxUsers) * 10.0) / 10.0) : 0.0);
            return dto;
        });

        return PageResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public List<LocationDto> getActiveLocations() {
        return locationRepository.findByActiveTrueAndDeletedFalse().stream()
                .map(LocationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationDto getLocationById(Long id) {
        WifiLocation loc = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));
        LocationDto dto = LocationDto.from(loc);
        if (sessionRepository != null) {
            long active = sessionRepository.countActiveSessionsByLocation(id, Instant.now());
            dto.setActiveSessionsCount(active);
            int maxUsers = loc.getMaxConcurrentUsers() != null ? loc.getMaxConcurrentUsers() : 50;
            dto.setCapacityUsagePercent(maxUsers > 0 ? Math.min(100.0, Math.round((active * 100.0 / maxUsers) * 10.0) / 10.0) : 0.0);
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public WifiLocation findEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));
    }

    @Transactional(readOnly = true)
    public WifiLocation findEntityBySlug(String slug) {
        return locationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "slug", slug));
    }

    @Transactional
    public LocationDto createLocation(CreateLocationRequest request, String actorUsername) {
        if (locationRepository.existsBySlugAndDeletedFalse(request.getSlug())) {
            throw new BadRequestException("A Wi-Fi location with slug '" + request.getSlug() + "' already exists");
        }

        WifiLocation location = new WifiLocation(
                request.getSlug(),
                request.getName(),
                request.getAddress(),
                request.getDescription(),
                request.getSessionDurationMinutes() != null ? request.getSessionDurationMinutes() : 120,
                request.getMaxBandwidthMbps() != null ? request.getMaxBandwidthMbps() : 20,
                request.getTermsAndConditions(),
                request.getWelcomeMessage() != null ? request.getWelcomeMessage() : "Welcome to High-Speed Wi-Fi",
                request.isActive());

        location.setPromoTitle(request.getPromoTitle());
        location.setPromoContent(request.getPromoContent());
        location.setPromoBannerUrl(request.getPromoBannerUrl());
        if (request.getBackgroundImageUrl() != null && !request.getBackgroundImageUrl().isBlank()) {
            location.setBackgroundImageUrl(request.getBackgroundImageUrl());
        }
        location.setPromoActionUrl(request.getPromoActionUrl());

        if (request.getAdminUnitId() != null) {
            AdministrativeUnit unit = adminUnitRepository.findById(request.getAdminUnitId())
                    .orElseThrow(() -> new BadRequestException(
                            "Không tìm thấy đơn vị địa bàn: " + request.getAdminUnitId()));
            location.setAdminUnit(unit);
        }
        location.setLocationCode(
                request.getLocationCode() != null ? request.getLocationCode() : request.getSlug().toUpperCase());
        if (request.getLatitude() != null && request.getLongitude() != null) {
            validateGeoFence(request.getLatitude(), request.getLongitude());
            location.setLatitude(request.getLatitude());
            location.setLongitude(request.getLongitude());
        }
        location.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        location.setIspName(request.getIspName());
        location.setCircuitCode(request.getCircuitCode());
        location.setContactName(request.getContactName());
        location.setContactPhone(request.getContactPhone());
        if (request.getMaxConcurrentUsers() != null) {
            location.setMaxConcurrentUsers(request.getMaxConcurrentUsers());
        } else {
            location.setMaxConcurrentUsers(50);
        }

        WifiLocation saved = locationRepository.save(location);

        auditService.recordEvent(
                AuditAction.LOCATION_CREATE,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "LOCATION",
                saved.getId().toString(),
                "Created new Wi-Fi location: " + saved.getName() + " (" + saved.getSlug() + ")");

        return LocationDto.from(saved);
    }

    @Transactional
    public LocationDto updateLocation(Long id, UpdateLocationRequest request, String actorUsername) {
        WifiLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        location.setName(request.getName());
        if (request.getAddress() != null)
            location.setAddress(request.getAddress());
        if (request.getDescription() != null)
            location.setDescription(request.getDescription());
        if (request.getSessionDurationMinutes() != null)
            location.setSessionDurationMinutes(request.getSessionDurationMinutes());
        if (request.getMaxBandwidthMbps() != null)
            location.setMaxBandwidthMbps(request.getMaxBandwidthMbps());
        if (request.getMaxConcurrentUsers() != null)
            location.setMaxConcurrentUsers(request.getMaxConcurrentUsers());
        if (request.getTermsAndConditions() != null)
            location.setTermsAndConditions(request.getTermsAndConditions());
        if (request.getWelcomeMessage() != null)
            location.setWelcomeMessage(request.getWelcomeMessage());
        if (request.getPromoTitle() != null)
            location.setPromoTitle(request.getPromoTitle());
        if (request.getPromoContent() != null)
            location.setPromoContent(request.getPromoContent());
        if (request.getPromoBannerUrl() != null)
            location.setPromoBannerUrl(request.getPromoBannerUrl());
        if (request.getBackgroundImageUrl() != null)
            location.setBackgroundImageUrl(request.getBackgroundImageUrl());
        if (request.getPromoActionUrl() != null)
            location.setPromoActionUrl(request.getPromoActionUrl());
        if (request.getActive() != null)
            location.setActive(request.getActive());
        if (request.getAdminUnitId() != null) {
            AdministrativeUnit unit = adminUnitRepository.findById(request.getAdminUnitId())
                    .orElseThrow(() -> new BadRequestException(
                            "Không tìm thấy đơn vị địa bàn: " + request.getAdminUnitId()));
            location.setAdminUnit(unit);
        }
        if (request.getLocationCode() != null)
            location.setLocationCode(request.getLocationCode());
        if (request.getLatitude() != null && request.getLongitude() != null) {
            validateGeoFence(request.getLatitude(), request.getLongitude());
            location.setLatitude(request.getLatitude());
            location.setLongitude(request.getLongitude());
        }
        if (request.getStatus() != null)
            location.setStatus(request.getStatus());
        if (request.getStatusReason() != null)
            location.setStatusReason(request.getStatusReason());
        if (request.getIspName() != null)
            location.setIspName(request.getIspName());
        if (request.getCircuitCode() != null)
            location.setCircuitCode(request.getCircuitCode());
        if (request.getContactName() != null)
            location.setContactName(request.getContactName());
        if (request.getContactPhone() != null)
            location.setContactPhone(request.getContactPhone());

        WifiLocation updated = locationRepository.save(location);

        auditService.recordEvent(
                AuditAction.LOCATION_UPDATE,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "LOCATION",
                updated.getId().toString(),
                "Updated Wi-Fi location settings for: " + updated.getName());

        return LocationDto.from(updated);
    }

    @Transactional
    public void deleteLocation(Long id, String actorUsername) {
        WifiLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        // Soft delete to protect legal log compliance and avoid foreign key integrity
        // violations
        location.setDeleted(true);
        location.setDeletedAt(java.time.Instant.now());
        location.setActive(false);
        location.setStatus("DELETED");
        location.setStatusReason("Đã xóa khỏi hệ thống bởi " + (actorUsername != null ? actorUsername : "admin"));

        // Free up unique constraints on slug and location_code for future re-creations
        String oldSlug = location.getSlug();
        location.setSlug(oldSlug + "-deleted-" + location.getId());
        if (location.getLocationCode() != null) {
            location.setLocationCode(location.getLocationCode() + "-DEL-" + location.getId());
        }

        locationRepository.save(location);

        auditService.recordEvent(
                AuditAction.LOCATION_DELETE,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "LOCATION",
                id.toString(),
                "Deleted Wi-Fi location: " + location.getName() + " (original slug: " + oldSlug + ")");
    }

    private void validateGeoFence(Double lat, Double lng) {
        // Hải Phòng bounding box approx: 20.50N to 21.15N, 106.40E to 107.15E
        if (lat < 20.50 || lat > 21.15 || lng < 106.40 || lng > 107.15) {
            throw new BadRequestException("Tọa độ (" + lat + ", " + lng
                    + ") nằm ngoài phạm vi ranh giới thành phố Hải Phòng (Vĩ độ: 20.50-21.15, Kinh độ: 106.40-107.15)");
        }
    }
}
