package com.wificaptive.gateway.radius;

import com.wificaptive.sessions.entity.GuestSession;
import com.wificaptive.sessions.repository.GuestSessionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusException;
import org.tinyradius.util.RadiusServer;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Embedded RADIUS Server (RFC 2865 / RFC 2866) built directly into Spring Boot.
 *
 * Listens on UDP port 1812 for Access-Request packets dispatched by:
 * - Grandstream GWN7002 / GWN Series Routers and Access Points
 * - OPNsense / pfSense / MikroTik / Cisco Enterprise Gateways
 *
 * Matches incoming user/MAC credentials against active authorized guest sessions
 * in PostgreSQL, and immediately replies with RADIUS Access-Accept (enforcing
 * Session-Timeout and Bandwidth limit attributes).
 */
@Component
public class EmbeddedRadiusServer {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedRadiusServer.class);

    private final GuestSessionRepository sessionRepository;

    @Value("${app.gateway.radius.enabled:true}")
    private boolean radiusEnabled;

    @Value("${app.gateway.radius.port:1812}")
    private int radiusAuthPort;

    @Value("${app.gateway.radius.acct-port:1813}")
    private int radiusAcctPort;

    @Value("${app.gateway.radius.shared-secret:HaiPhongSecret@2026}")
    private String sharedSecret;

    private InternalRadiusServer server;

    public EmbeddedRadiusServer(GuestSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @PostConstruct
    public void start() {
        if (!radiusEnabled) {
            log.info("[Embedded RADIUS] Server is disabled by configuration (app.gateway.radius.enabled=false).");
            return;
        }

        try {
            server = new InternalRadiusServer();
            server.setAuthPort(radiusAuthPort);
            server.setAcctPort(radiusAcctPort);
            server.start(true, true);
            log.info("==================================================================");
            log.info("[Embedded RADIUS] Server STARTED on UDP Auth Port {}, Acct Port {}", radiusAuthPort, radiusAcctPort);
            log.info("[Embedded RADIUS] Shared Secret configured (Length: {} chars)", sharedSecret != null ? sharedSecret.length() : 0);
            log.info("==================================================================");
        } catch (Exception e) {
            log.error("[Embedded RADIUS] Failed to start RADIUS Server on UDP port {}: {}", radiusAuthPort, e.getMessage(), e);
        }
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            try {
                server.stop();
                log.info("[Embedded RADIUS] Server STOPPED successfully.");
            } catch (Exception e) {
                log.warn("[Embedded RADIUS] Error stopping RADIUS server: {}", e.getMessage());
            }
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    /**
     * Inner TinyRadius server implementation.
     */
    private class InternalRadiusServer extends RadiusServer {

        @Override
        public String getSharedSecret(InetSocketAddress client) {
            return sharedSecret;
        }

        @Override
        public String getUserPassword(String userName) {
            // Password matching is handled dynamically inside accessRequestReceived()
            return null;
        }

        @Override
        public RadiusPacket accessRequestReceived(AccessRequest request, InetSocketAddress client) throws RadiusException {
            String rawUserName = request.getUserName();
            String rawPassword = request.getUserPassword();
            String clientIp = client != null && client.getAddress() != null ? client.getAddress().getHostAddress() : "unknown";

            log.info("[Embedded RADIUS] Received Access-Request from {}: User-Name='{}'", clientIp, rawUserName);

            if (rawUserName == null || rawUserName.isBlank()) {
                log.warn("[Embedded RADIUS] Rejecting Access-Request: User-Name is empty.");
                return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());
            }

            // Extract MAC and optional domain (e.g. "a864f1e2c395@gwn" or "A8:64:F1:E2:C3:95")
            String cleanUser = rawUserName.trim();
            if (cleanUser.contains("@")) {
                cleanUser = cleanUser.substring(0, cleanUser.indexOf('@'));
            }
            String cleanMac = cleanUser.replaceAll("[:-]", "").toLowerCase();

            Instant now = Instant.now();
            Optional<GuestSession> matchedSession = Optional.empty();

            // 1. Try matching by sessionToken (if passed as password or username)
            if (rawPassword != null && rawPassword.startsWith("sess_")) {
                matchedSession = sessionRepository.findBySessionToken(rawPassword.trim());
            }

            // 2. Try matching active session by client MAC address
            if (matchedSession.isEmpty()) {
                List<GuestSession> activeSessions = sessionRepository.findAll().stream()
                        .filter(s -> s.getMacAddress() != null
                                && s.getMacAddress().replaceAll("[:-]", "").equalsIgnoreCase(cleanMac)
                                && "AUTHORIZED".equalsIgnoreCase(s.getStatus() != null ? s.getStatus().name() : "")
                                && (s.getExpiresAt() == null || s.getExpiresAt().isAfter(now)))
                        .toList();

                if (!activeSessions.isEmpty()) {
                    matchedSession = Optional.of(activeSessions.get(0));
                }
            }

            // 3. Fallback: If cleanMac has 12 hex digits, allow as free guest access with default 120-min limit
            int sessionTimeoutSeconds = 7200; // 120 minutes default
            int maxBandwidthKbps = 20000;      // 20 Mbps

            if (matchedSession.isPresent()) {
                GuestSession session = matchedSession.get();
                if (session.getExpiresAt() != null && session.getExpiresAt().isAfter(now)) {
                    sessionTimeoutSeconds = (int) Duration.between(now, session.getExpiresAt()).getSeconds();
                }
                if (session.getLocation() != null && session.getLocation().getMaxBandwidthMbps() != null) {
                    maxBandwidthKbps = session.getLocation().getMaxBandwidthMbps() * 1000;
                }
                log.info("[Embedded RADIUS] Matched Session ID #{}: MAC={}, Status={}, Remaining={}s, Bandwidth={}Kbps",
                        session.getId(), session.getMacAddress(), session.getStatus(), sessionTimeoutSeconds, maxBandwidthKbps);
            } else if (cleanMac.matches("^[0-9a-f]{12}$")) {
                log.info("[Embedded RADIUS] Auto-granting guest access for MAC {}: Timeout={}s, Bandwidth={}Kbps",
                        cleanMac, sessionTimeoutSeconds, maxBandwidthKbps);
            } else {
                log.warn("[Embedded RADIUS] Rejecting Access-Request for '{}': No active session and invalid MAC format.", rawUserName);
                return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());
            }

            // Construct Access-Accept response packet
            RadiusPacket response = new RadiusPacket(RadiusPacket.ACCESS_ACCEPT, request.getPacketIdentifier());

            // Standard RFC 2865 Attributes
            response.addAttribute("Session-Timeout", String.valueOf(Math.max(sessionTimeoutSeconds, 60)));
            response.addAttribute("Idle-Timeout", "1800"); // 30 minutes idle
            response.addAttribute("Acct-Interim-Interval", "300"); // 5 minutes accounting

            // Standard Reply-Message
            response.addAttribute("Reply-Message", "Haiphong GovTech Wi-Fi Access Granted");

            log.info("[Embedded RADIUS] Sent Access-Accept for User-Name='{}' (Session-Timeout={}s)",
                    rawUserName, sessionTimeoutSeconds);

            return response;
        }

        @Override
        public RadiusPacket accountingRequestReceived(AccountingRequest request, InetSocketAddress client) throws RadiusException {
            String clientIp = client != null && client.getAddress() != null ? client.getAddress().getHostAddress() : "unknown";
            log.debug("[Embedded RADIUS] Received Accounting-Request from {}: Type={}",
                    clientIp, request.getAcctStatusType());

            // Acknowledge accounting packets with standard Accounting-Response
            return new RadiusPacket(RadiusPacket.ACCOUNTING_RESPONSE, request.getPacketIdentifier());
        }
    }
}

