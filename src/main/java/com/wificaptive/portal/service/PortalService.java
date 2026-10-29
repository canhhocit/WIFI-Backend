package com.wificaptive.portal.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.GatewayException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.devices.entity.Device;
import com.wificaptive.devices.service.DeviceService;
import com.wificaptive.gateway.adapter.GatewayAdapter;
import com.wificaptive.gateway.dto.GatewayAuthResult;
import com.wificaptive.gateway.dto.GatewayRevokeResult;
import com.wificaptive.locations.dto.LocationDto;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.portal.dto.GuestLoginRequest;
import com.wificaptive.portal.dto.GuestLoginResponse;
import com.wificaptive.portal.dto.GuestSessionStatusResponse;
import com.wificaptive.portal.dto.PortalInitResponse;
import com.wificaptive.sessions.entity.GuestSession;
import com.wificaptive.sessions.entity.SessionStatus;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PortalService {

    private static final Logger log = LoggerFactory.getLogger(PortalService.class);
    private static final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    private final WifiLocationRepository locationRepository;
    private final GuestSessionRepository sessionRepository;
    private final DeviceService deviceService;
    private final GatewayAdapter gatewayAdapter;
    private final AuditService auditService;
    private final com.wificaptive.campaigns.service.CampaignService campaignService;
    private final com.wificaptive.surveys.service.SurveyService surveyService;
    private final com.wificaptive.verification.service.OtpVerificationService otpVerificationService;
    private final com.wificaptive.portal.template.service.PortalTemplateService portalTemplateService;
    private final com.wificaptive.network.service.MacBlacklistService macBlacklistService;

    public PortalService(
            WifiLocationRepository locationRepository,
            GuestSessionRepository sessionRepository,
            DeviceService deviceService,
            GatewayAdapter gatewayAdapter,
            AuditService auditService,
            com.wificaptive.campaigns.service.CampaignService campaignService,
            com.wificaptive.surveys.service.SurveyService surveyService,
            com.wificaptive.verification.service.OtpVerificationService otpVerificationService,
            com.wificaptive.portal.template.service.PortalTemplateService portalTemplateService,
            com.wificaptive.network.service.MacBlacklistService macBlacklistService) {
        this.locationRepository = locationRepository;
        this.sessionRepository = sessionRepository;
        this.deviceService = deviceService;
        this.gatewayAdapter = gatewayAdapter;
        this.auditService = auditService;
        this.campaignService = campaignService;
        this.surveyService = surveyService;
        this.otpVerificationService = otpVerificationService;
        this.portalTemplateService = portalTemplateService;
        this.macBlacklistService = macBlacklistService;
    }

    @Transactional(readOnly = true)
    public PortalInitResponse getPortalInit(String locationSlug, HttpServletRequest request) {
        WifiLocation location;
        if (StringUtils.hasText(locationSlug)) {
            location = locationRepository.findBySlug(locationSlug.trim())
                    .orElse(null);
        } else {
            List<WifiLocation> activeLocations = locationRepository.findByActiveTrue();
            location = activeLocations.isEmpty() ? null : activeLocations.get(0);
        }

        if (location == null || !location.isActive()) {
            throw new ResourceNotFoundException("Active Wi-Fi location not found for captive portal initialization");
        }

        String clientIp = resolveClientIp(request);
        String clientMacHint = resolveClientMac(request, null);

        boolean isMock = gatewayAdapter.getStatus().isSimulated();
        String notice = isMock
                ? "Simulated Gateway Mode: Network authorization is simulated for MVP testing. Physical firewall access has not been modified."
                : "Live Gateway Connected.";

        PortalInitResponse response = new PortalInitResponse(
                LocationDto.from(location),
                clientIp,
                clientMacHint,
                isMock,
                notice
        );

        // Link active policy/media campaign
        if (campaignService != null) {
            com.wificaptive.campaigns.entity.Campaign activeCampaign = campaignService.getActiveCampaignForLocation(
                    location.getId(),
                    location.getAdminUnitId()
            );
            if (activeCampaign != null) {
                response.setActiveCampaign(com.wificaptive.campaigns.dto.CampaignDto.from(activeCampaign));
                if (surveyService != null) {
                    response.setActiveSurvey(surveyService.getActiveSurveyForCampaign(activeCampaign.getId()));
                }
            }
        }

        // Link template
        if (portalTemplateService != null) {
            response.setPortalTemplate(portalTemplateService.getDefaultOrActive());
        }

        // Check if MAC is remembered (can bypass OTP)
        boolean isRemembered = otpVerificationService != null && otpVerificationService.isMacRemembered(clientMacHint);
        response.setMacRemembered(isRemembered);
        response.setOtpRequired(!isRemembered);

        return response;
    }

    @Transactional
    public GuestLoginResponse authenticateGuest(GuestLoginRequest request, HttpServletRequest httpRequest) {
        if (!request.isConsentGiven()) {
            throw new BadRequestException("You must accept the Terms and Conditions to access the network.");
        }

        WifiLocation location = locationRepository.findBySlug(request.getLocationSlug().trim())
                .orElseThrow(() -> new ResourceNotFoundException("WifiLocation", "slug", request.getLocationSlug()));

        if (!location.isActive()) {
            throw new BadRequestException("This Wi-Fi location is currently inactive.");
        }

        String clientIp = resolveClientIp(httpRequest);
        String macAddress = resolveClientMac(httpRequest, request.getMacAddress());
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "Unknown";

        // Track and inspect device
        Device device = deviceService.registerOrUpdateDevice(macAddress, clientIp, userAgent);
        if (device.isBlocked()) {
            throw new BadRequestException("This device (MAC: " + macAddress + ") has been blocked by network administration.");
        }

        // Check MAC Blacklist
        if (macBlacklistService != null && macBlacklistService.isBlacklisted(macAddress)) {
            throw new BadRequestException("Thiết bị có địa chỉ MAC " + macAddress + " nằm trong danh sách cấm truy cập (Blacklist) của cơ quan an ninh mạng.");
        }

        // Close any prior active sessions for this device
        List<GuestSession> existingActive = sessionRepository.findActiveSessionsByMac(macAddress, Instant.now());
        for (GuestSession prior : existingActive) {
            log.info("Closing previous session {} for MAC {} due to re-authentication", prior.getId(), macAddress);
            gatewayAdapter.revokeSession(prior.getId(), macAddress, clientIp, prior.getGatewayReference());
            prior.setStatus(SessionStatus.EXPIRED);
            sessionRepository.save(prior);
        }

        // 1. Create session in PENDING state
        Instant now = Instant.now();
        String sessionToken = "sess_" + UUID.randomUUID().toString().replace("-", "");

        GuestSession session = new GuestSession();
        session.setSessionToken(sessionToken);
        session.setLocation(location);
        session.setDevice(device);
        session.setMacAddress(macAddress);
        session.setIpAddress(clientIp);
        session.setGuestName(StringUtils.hasText(request.getGuestName()) ? request.getGuestName().trim() : "Guest User");
        session.setGuestContact(StringUtils.hasText(request.getGuestContact()) ? request.getGuestContact().trim() : null);
        session.setConsentGiven(true);
        session.setConsentTimestamp(now);
        session.setStatus(SessionStatus.PENDING);

        GuestSession savedSession = sessionRepository.save(session);

        // 2. Invoke Gateway Adapter for network authorization
        log.info("Requesting gateway authorization for session {} (MAC: {}, IP: {}, Duration: {}m)...",
                savedSession.getId(), macAddress, clientIp, location.getSessionDurationMinutes());

        GatewayAuthResult authResult = gatewayAdapter.authorizeSession(
                savedSession.getId(),
                macAddress,
                clientIp,
                location.getSessionDurationMinutes(),
                location.getMaxBandwidthMbps()
        );

        GuestLoginResponse response = new GuestLoginResponse();
        response.setSessionId(savedSession.getId());
        response.setSessionToken(savedSession.getSessionToken());
        response.setLocationName(location.getName());
        response.setDurationMinutes(location.getSessionDurationMinutes());

        if (authResult.isSuccess()) {
            savedSession.setStatus(SessionStatus.AUTHORIZED);
            savedSession.setAuthorizedAt(now);
            savedSession.setExpiresAt(now.plus(location.getSessionDurationMinutes(), ChronoUnit.MINUTES));
            savedSession.setGatewayReference(authResult.getGatewayReference());
            sessionRepository.save(savedSession);

            response.setStatus(SessionStatus.AUTHORIZED);
            response.setSimulated(authResult.isSimulated());
            response.setSimulatedNotice(authResult.isSimulated()
                    ? "Authorization simulated in Mock Mode. Real Internet connectivity requires physical gateway enforcement (OPNsense)."
                    : "Network access authorized by gateway.");
            response.setExpiresAt(savedSession.getExpiresAt());
            response.setMessage("Internet access granted. Welcome to " + location.getName() + "!");

            auditService.recordEvent(
                    AuditAction.SESSION_CREATE,
                    "guest:" + macAddress,
                    clientIp,
                    "SESSION",
                    savedSession.getId().toString(),
                    "Guest session authorized via " + (authResult.isSimulated() ? "Mock Gateway" : "Live Gateway")
            );

            return response;
        } else {
            savedSession.setStatus(SessionStatus.FAILED);
            savedSession.setFailureReason(authResult.getMessage());
            sessionRepository.save(savedSession);

            response.setStatus(SessionStatus.FAILED);
            response.setSimulated(authResult.isSimulated());
            response.setSimulatedNotice(authResult.getMessage());
            response.setMessage("Gateway authorization failed: " + authResult.getMessage());

            throw new GatewayException("Gateway rejected session authorization: " + authResult.getMessage(), authResult.getStatusCode());
        }
    }

    @Transactional
    public GuestSessionStatusResponse getGuestSessionStatus(String token) {
        GuestSession session = sessionRepository.findBySessionToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("GuestSession", "token", token));

        Instant now = Instant.now();
        // Check dynamic expiration
        if (session.getStatus() == SessionStatus.AUTHORIZED && session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            log.info("Session {} token {} reached TTL, marking as EXPIRED", session.getId(), token);
            gatewayAdapter.revokeSession(session.getId(), session.getMacAddress(), session.getIpAddress(), session.getGatewayReference());
            session.setStatus(SessionStatus.EXPIRED);
            sessionRepository.save(session);
        }

        GuestSessionStatusResponse resp = new GuestSessionStatusResponse();
        resp.setSessionToken(session.getSessionToken());
        resp.setStatus(session.getStatus());
        resp.setSimulated(gatewayAdapter.getStatus().isSimulated());
        resp.setSimulatedNotice(resp.isSimulated()
                ? "Simulated Gateway: Network session tracked in database. Physical router enforcement not engaged."
                : "Active hardware gateway enforcement.");

        if (session.getStatus() == SessionStatus.AUTHORIZED && session.getExpiresAt() != null) {
            long remaining = (session.getExpiresAt().toEpochMilli() - now.toEpochMilli()) / 1000;
            resp.setRemainingSeconds(Math.max(0, remaining));
        } else {
            resp.setRemainingSeconds(0L);
        }

        resp.setExpiresAt(session.getExpiresAt());
        resp.setAuthorizedAt(session.getAuthorizedAt());
        resp.setLocationName(session.getLocation() != null ? session.getLocation().getName() : "Public Wi-Fi");
        resp.setMacAddress(session.getMacAddress());
        resp.setIpAddress(session.getIpAddress());
        resp.setMaxBandwidthMbps(session.getLocation() != null ? session.getLocation().getMaxBandwidthMbps() : 20);
        resp.setRevokeReason(session.getRevokeReason());

        return resp;
    }

    @Transactional
    public GuestSessionStatusResponse disconnectGuestSession(String token) {
        GuestSession session = sessionRepository.findBySessionToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("GuestSession", "token", token));

        if (session.getStatus() == SessionStatus.AUTHORIZED) {
            gatewayAdapter.revokeSession(session.getId(), session.getMacAddress(), session.getIpAddress(), session.getGatewayReference());
            session.setStatus(SessionStatus.REVOKED);
            session.setRevokedAt(Instant.now());
            session.setRevokedBy("guest");
            session.setRevokeReason("Voluntary guest disconnect from session status page");
            sessionRepository.save(session);

            auditService.recordEvent(
                    AuditAction.SESSION_REVOKE,
                    "guest:" + session.getMacAddress(),
                    session.getIpAddress(),
                    "SESSION",
                    session.getId().toString(),
                    "Guest disconnected session from status page"
            );
        }

        return getGuestSessionStatus(token);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return (ip != null && !ip.isBlank()) ? ip : "127.0.0.1";
    }

    private String resolveClientMac(HttpServletRequest request, String requestedMac) {
        if (request != null) {
            String headerMac = request.getHeader("X-Client-MAC");
            if (StringUtils.hasText(headerMac) && MAC_PATTERN.matcher(headerMac.trim()).matches()) {
                return headerMac.trim().toLowerCase();
            }
        }

        if (StringUtils.hasText(requestedMac) && MAC_PATTERN.matcher(requestedMac.trim()).matches()) {
            return requestedMac.trim().toLowerCase();
        }

        // Deterministic fallback for local browser testing
        String ip = resolveClientIp(request);
        int hash = Math.abs(ip.hashCode() % 0xFF);
        return String.format("02:42:ac:11:00:%02x", hash);
    }
}
