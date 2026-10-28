package com.wificaptive.locations.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.locations.dto.CreateLocationRequest;
import com.wificaptive.locations.dto.LocationDto;
import com.wificaptive.locations.dto.UpdateLocationRequest;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class LocationService {

    private final WifiLocationRepository locationRepository;
    private final AuditService auditService;

    public LocationService(WifiLocationRepository locationRepository, AuditService auditService) {
        this.locationRepository = locationRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<LocationDto> getLocations(String query, Pageable pageable) {
        Page<WifiLocation> page;
        if (StringUtils.hasText(query)) {
            page = locationRepository.findByNameContainingIgnoreCase(query.trim(), pageable);
        } else {
            page = locationRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(LocationDto::from));
    }

    @Transactional(readOnly = true)
    public List<LocationDto> getActiveLocations() {
        return locationRepository.findByActiveTrue().stream()
                .map(LocationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationDto getLocationById(Long id) {
        WifiLocation loc = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));
        return LocationDto.from(loc);
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
        if (locationRepository.existsBySlug(request.getSlug())) {
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
                request.isActive()
        );

        location.setPromoTitle(request.getPromoTitle());
        location.setPromoContent(request.getPromoContent());
        location.setPromoBannerUrl(request.getPromoBannerUrl());
        location.setPromoActionUrl(request.getPromoActionUrl());

        WifiLocation saved = locationRepository.save(location);

        auditService.recordEvent(
                AuditAction.LOCATION_CREATE,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "LOCATION",
                saved.getId().toString(),
                "Created new Wi-Fi location: " + saved.getName() + " (" + saved.getSlug() + ")"
        );

        return LocationDto.from(saved);
    }

    @Transactional
    public LocationDto updateLocation(Long id, UpdateLocationRequest request, String actorUsername) {
        WifiLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        location.setName(request.getName());
        if (request.getAddress() != null) location.setAddress(request.getAddress());
        if (request.getDescription() != null) location.setDescription(request.getDescription());
        if (request.getSessionDurationMinutes() != null) location.setSessionDurationMinutes(request.getSessionDurationMinutes());
        if (request.getMaxBandwidthMbps() != null) location.setMaxBandwidthMbps(request.getMaxBandwidthMbps());
        if (request.getTermsAndConditions() != null) location.setTermsAndConditions(request.getTermsAndConditions());
        if (request.getWelcomeMessage() != null) location.setWelcomeMessage(request.getWelcomeMessage());
        if (request.getPromoTitle() != null) location.setPromoTitle(request.getPromoTitle());
        if (request.getPromoContent() != null) location.setPromoContent(request.getPromoContent());
        if (request.getPromoBannerUrl() != null) location.setPromoBannerUrl(request.getPromoBannerUrl());
        if (request.getPromoActionUrl() != null) location.setPromoActionUrl(request.getPromoActionUrl());
        if (request.getActive() != null) location.setActive(request.getActive());

        WifiLocation updated = locationRepository.save(location);

        auditService.recordEvent(
                AuditAction.LOCATION_UPDATE,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "LOCATION",
                updated.getId().toString(),
                "Updated Wi-Fi location settings for: " + updated.getName()
        );

        return LocationDto.from(updated);
    }

    @Transactional
    public void deleteLocation(Long id, String actorUsername) {
        WifiLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        locationRepository.delete(location);

        auditService.recordEvent(
                AuditAction.LOCATION_DELETE,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "LOCATION",
                id.toString(),
                "Deleted Wi-Fi location: " + location.getName()
        );
    }
}
