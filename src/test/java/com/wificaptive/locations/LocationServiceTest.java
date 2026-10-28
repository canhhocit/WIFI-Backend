package com.wificaptive.locations;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.locations.dto.CreateLocationRequest;
import com.wificaptive.locations.dto.LocationDto;
import com.wificaptive.locations.dto.UpdateLocationRequest;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.locations.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private WifiLocationRepository locationRepository;

    @Mock
    private AuditService auditService;

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new LocationService(locationRepository, auditService);
    }

    @Test
    void testCreateLocation_Success() {
        CreateLocationRequest request = new CreateLocationRequest();
        request.setSlug("subway-terminal");
        request.setName("Subway Station Terminal");
        request.setSessionDurationMinutes(90);
        request.setMaxBandwidthMbps(30);
        request.setTermsAndConditions("Sample terms");
        request.setActive(true);

        when(locationRepository.existsBySlug("subway-terminal")).thenReturn(false);

        WifiLocation saved = new WifiLocation();
        saved.setId(10L);
        saved.setSlug("subway-terminal");
        saved.setName("Subway Station Terminal");
        saved.setSessionDurationMinutes(90);
        saved.setMaxBandwidthMbps(30);
        saved.setTermsAndConditions("Sample terms");
        saved.setActive(true);

        when(locationRepository.save(any(WifiLocation.class))).thenReturn(saved);

        LocationDto dto = locationService.createLocation(request, "admin");

        assertNotNull(dto);
        assertEquals("subway-terminal", dto.getSlug());
        assertEquals("Subway Station Terminal", dto.getName());
        assertEquals(90, dto.getSessionDurationMinutes());
    }

    @Test
    void testCreateLocation_DuplicateSlugThrowsBadRequest() {
        CreateLocationRequest request = new CreateLocationRequest();
        request.setSlug("existing-slug");
        request.setName("Existing Branch");
        request.setTermsAndConditions("Terms");

        when(locationRepository.existsBySlug("existing-slug")).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                locationService.createLocation(request, "admin"));

        verify(locationRepository, never()).save(any());
    }

    @Test
    void testUpdateLocation_Success() {
        WifiLocation existing = new WifiLocation();
        existing.setId(5L);
        existing.setSlug("downtown-hub");
        existing.setName("Old Name");
        existing.setSessionDurationMinutes(60);
        existing.setTermsAndConditions("Terms");

        when(locationRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(locationRepository.save(any(WifiLocation.class))).thenAnswer(i -> i.getArgument(0));

        UpdateLocationRequest update = new UpdateLocationRequest();
        update.setName("New Central Hub");
        update.setSessionDurationMinutes(120);
        update.setTermsAndConditions("Updated terms");
        update.setActive(true);

        LocationDto result = locationService.updateLocation(5L, update, "admin");

        assertEquals("New Central Hub", result.getName());
        assertEquals(120, result.getSessionDurationMinutes());
        assertEquals("Updated terms", result.getTermsAndConditions());
    }
}
