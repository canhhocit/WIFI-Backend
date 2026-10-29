package com.wificaptive.portal;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.GatewayException;
import com.wificaptive.devices.entity.Device;
import com.wificaptive.devices.service.DeviceService;
import com.wificaptive.gateway.adapter.GatewayAdapter;
import com.wificaptive.gateway.dto.GatewayAuthResult;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.portal.dto.GuestLoginRequest;
import com.wificaptive.portal.dto.GuestLoginResponse;
import com.wificaptive.portal.service.PortalService;
import com.wificaptive.sessions.entity.GuestSession;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalServiceTest {

    @Mock
    private WifiLocationRepository locationRepository;

    @Mock
    private GuestSessionRepository sessionRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private GatewayAdapter gatewayAdapter;

    @Mock
    private AuditService auditService;

    @Mock
    private com.wificaptive.campaigns.service.CampaignService campaignService;

    @Mock
    private com.wificaptive.surveys.service.SurveyService surveyService;

    @Mock
    private com.wificaptive.verification.service.OtpVerificationService otpVerificationService;

    @Mock
    private com.wificaptive.portal.template.service.PortalTemplateService portalTemplateService;

    @Mock
    private com.wificaptive.network.service.MacBlacklistService macBlacklistService;

    private PortalService portalService;

    @BeforeEach
    void setUp() {
        portalService = new PortalService(
                locationRepository,
                sessionRepository,
                deviceService,
                gatewayAdapter,
                auditService,
                campaignService,
                surveyService,
                otpVerificationService,
                portalTemplateService,
                macBlacklistService
        );
    }

    @Test
    void testConsentValidation_ThrowsExceptionWhenConsentFalse() {
        GuestLoginRequest request = new GuestLoginRequest();
        request.setLocationSlug("test-location");
        request.setConsentGiven(false);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        assertThrows(BadRequestException.class, () ->
                portalService.authenticateGuest(request, httpRequest));
    }

    @Test
    void testAuthenticateGuest_SuccessFlow() {
        WifiLocation location = new WifiLocation();
        location.setId(1L);
        location.setName("Downtown Coffee");
        location.setSlug("downtown-coffee");
        location.setActive(true);
        location.setSessionDurationMinutes(90);
        location.setMaxBandwidthMbps(25);

        when(locationRepository.findBySlug("downtown-coffee")).thenReturn(Optional.of(location));

        Device device = new Device("00:11:22:33:44:55", "192.168.1.100", "Client-4455", "Mozilla", "iOS");
        when(deviceService.registerOrUpdateDevice(anyString(), anyString(), any())).thenReturn(device);

        when(sessionRepository.findActiveSessionsByMac(anyString(), any())).thenReturn(Collections.emptyList());

        GuestSession pendingSession = new GuestSession();
        pendingSession.setId(501L);
        pendingSession.setSessionToken("sess_token_123");
        pendingSession.setLocation(location);
        pendingSession.setStatus(SessionStatus.PENDING);

        when(sessionRepository.save(any(GuestSession.class))).thenReturn(pendingSession);

        GatewayAuthResult authResult = GatewayAuthResult.success("MOCK-GW-AUTH-TEST", true, "Authorized", 10);
        when(gatewayAdapter.authorizeSession(eq(501L), anyString(), anyString(), eq(90), eq(25)))
                .thenReturn(authResult);

        GuestLoginRequest loginRequest = new GuestLoginRequest();
        loginRequest.setLocationSlug("downtown-coffee");
        loginRequest.setMacAddress("00:11:22:33:44:55");
        loginRequest.setGuestName("Test Guest");
        loginRequest.setConsentGiven(true);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("192.168.1.100");

        GuestLoginResponse response = portalService.authenticateGuest(loginRequest, httpRequest);

        assertNotNull(response);
        assertEquals(SessionStatus.AUTHORIZED, response.getStatus());
        assertTrue(response.isSimulated());
        assertEquals("sess_token_123", response.getSessionToken());
        assertEquals("Downtown Coffee", response.getLocationName());

        ArgumentCaptor<GuestSession> sessionCaptor = ArgumentCaptor.forClass(GuestSession.class);
        verify(sessionRepository, atLeast(2)).save(sessionCaptor.capture());

        GuestSession finalSaved = sessionCaptor.getValue();
        assertEquals(SessionStatus.AUTHORIZED, finalSaved.getStatus());
        assertEquals("MOCK-GW-AUTH-TEST", finalSaved.getGatewayReference());
    }

    @Test
    void testAuthenticateGuest_GatewayFailureFlow() {
        WifiLocation location = new WifiLocation();
        location.setId(1L);
        location.setSlug("downtown-coffee");
        location.setActive(true);
        location.setSessionDurationMinutes(60);
        location.setMaxBandwidthMbps(10);

        when(locationRepository.findBySlug("downtown-coffee")).thenReturn(Optional.of(location));

        Device device = new Device("00:11:22:33:44:55", "192.168.1.100", "Client-4455", "Mozilla", "Android");
        when(deviceService.registerOrUpdateDevice(anyString(), anyString(), any())).thenReturn(device);

        GuestSession pendingSession = new GuestSession();
        pendingSession.setId(502L);
        pendingSession.setSessionToken("sess_token_failed");
        pendingSession.setLocation(location);
        pendingSession.setStatus(SessionStatus.PENDING);

        when(sessionRepository.save(any(GuestSession.class))).thenReturn(pendingSession);

        GatewayAuthResult authFail = GatewayAuthResult.failure("OPNsense connection timed out", 504, true, 25);
        when(gatewayAdapter.authorizeSession(eq(502L), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(authFail);

        GuestLoginRequest loginRequest = new GuestLoginRequest();
        loginRequest.setLocationSlug("downtown-coffee");
        loginRequest.setMacAddress("00:11:22:33:44:55");
        loginRequest.setConsentGiven(true);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        GatewayException ex = assertThrows(GatewayException.class, () ->
                portalService.authenticateGuest(loginRequest, httpRequest));

        assertTrue(ex.getMessage().contains("Gateway rejected session authorization"));
        assertEquals(SessionStatus.FAILED, pendingSession.getStatus());
        assertEquals("OPNsense connection timed out", pendingSession.getFailureReason());
    }

    @Test
    void testAuthenticateGuest_BlacklistedDeviceRejected() {
        WifiLocation location = new WifiLocation();
        location.setId(1L);
        location.setSlug("downtown-coffee");
        location.setActive(true);

        when(locationRepository.findBySlug("downtown-coffee")).thenReturn(Optional.of(location));

        Device device = new Device("AA:BB:CC:DD:EE:99", "192.168.1.100", "Attacker", "Mozilla", "Linux");
        when(deviceService.registerOrUpdateDevice(anyString(), anyString(), any())).thenReturn(device);
        when(macBlacklistService.isBlacklisted(anyString())).thenReturn(true);

        GuestLoginRequest loginRequest = new GuestLoginRequest();
        loginRequest.setLocationSlug("downtown-coffee");
        loginRequest.setMacAddress("AA:BB:CC:DD:EE:99");
        loginRequest.setConsentGiven(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                portalService.authenticateGuest(loginRequest, new MockHttpServletRequest()));

        assertTrue(ex.getMessage().contains("Blacklist"));
    }
}
