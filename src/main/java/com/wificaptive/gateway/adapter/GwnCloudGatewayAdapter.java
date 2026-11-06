package com.wificaptive.gateway.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.gateway.dto.GatewayAuthResult;
import com.wificaptive.gateway.dto.GatewayRevokeResult;
import com.wificaptive.gateway.dto.GatewayStatusDto;
import com.wificaptive.gateway.dto.MockGatewayConfigDto;
import com.wificaptive.gateway.entity.GatewayTransaction;
import com.wificaptive.gateway.repository.GatewayTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gateway Adapter for Grandstream GWN Series (GWN7001, GWN7000 routers, GWN APs)
 * managed via Grandstream GWN Cloud (https://www.gwn.cloud) or on-premise GWN Manager.
 *
 * Supports:
 * 1. OAuth / API Token acquisition via App ID & App Secret (GWN Cloud Open API v1.0).
 * 2. Real hardware enforcement: Authorize client MAC with duration and bandwidth policies.
 * 3. Client session revocation: Disconnect client MAC immediately upon expiration or admin request.
 * 4. Graceful Standby Mode: If App ID / Secret are not yet configured, operates in
 *    Pre-Integration Standby Mode so the system remains fully functional until keys are pasted.
 */
@Component("gwnCloudGatewayAdapter")
public class GwnCloudGatewayAdapter implements GatewayAdapter {

    private static final Logger log = LoggerFactory.getLogger(GwnCloudGatewayAdapter.class);

    private final GatewayTransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.gateway.gwn-cloud.base-url:https://www.gwn.cloud}")
    private String gwnBaseUrl;

    @Value("${app.gateway.gwn-cloud.app-id:}")
    private String appId;

    @Value("${app.gateway.gwn-cloud.app-secret:}")
    private String appSecret;

    @Value("${app.gateway.gwn-cloud.network-id:}")
    private String networkId;

    // In-memory token cache
    private volatile String cachedToken = null;
    private volatile Instant tokenExpiresAt = Instant.MIN;

    private final AtomicLong totalAuthorizations = new AtomicLong(0);
    private final AtomicLong totalRevocations = new AtomicLong(0);
    private final AtomicLong failedOperations = new AtomicLong(0);

    public GwnCloudGatewayAdapter(GatewayTransactionRepository transactionRepository, ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public GatewayAuthResult authorizeSession(
            Long sessionId,
            String macAddress,
            String ipAddress,
            Integer durationMinutes,
            Integer maxBandwidthMbps
    ) {
        long startTime = System.currentTimeMillis();
        totalAuthorizations.incrementAndGet();

        int duration = (durationMinutes != null && durationMinutes > 0) ? durationMinutes : 120;
        int bandwidth = (maxBandwidthMbps != null && maxBandwidthMbps > 0) ? maxBandwidthMbps : 20;

        // Check if real credentials are provided
        if (!isLiveCredentialsConfigured()) {
            long latencyMs = System.currentTimeMillis() - startTime;
            String simRef = "GWN7001-STBY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("[GWN Cloud Gateway] Device authorization (STANDBY MODE - Ready for GWN7001 integration): " +
                     "session={}, MAC={}, IP={}, duration={}m, bandwidth={}Mbps. Ref={}",
                    sessionId, macAddress, ipAddress, duration, bandwidth, simRef);

            saveTransaction(sessionId, "AUTHORIZE", true, 200, null,
                    String.format("{\"gatewayRef\":\"%s\",\"status\":\"STANDBY_MODE\",\"mac\":\"%s\",\"ip\":\"%s\"}", simRef, macAddress, ipAddress),
                    latencyMs);
            return GatewayAuthResult.success(simRef, true, "Cấp quyền truy cập Wi-Fi Nhà văn hóa (GWN7001 Sẵn sàng)", latencyMs);
        }

        // Real Grandstream GWN Cloud Open API Call
        try {
            String token = obtainGwnCloudToken();
            String referenceCode = "GWN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Map<String, Object> payload = new HashMap<>();
            payload.put("mac", formatMacAddress(macAddress));
            if (StringUtils.hasText(ipAddress)) {
                payload.put("ip", ipAddress);
            }
            payload.put("duration", duration);
            payload.put("rateLimitUp", bandwidth * 1024); // kbps
            payload.put("rateLimitDown", bandwidth * 1024); // kbps
            if (StringUtils.hasText(networkId)) {
                payload.put("networkId", networkId);
            }

            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(gwnBaseUrl.replaceAll("/+$", "") + "/api/v1.0/guest/authorize"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - startTime;

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[GWN Cloud Gateway] Successfully authorized MAC {} on GWN7001 router (Latency: {}ms)", macAddress, latencyMs);
                saveTransaction(sessionId, "AUTHORIZE", true, response.statusCode(), null, response.body(), latencyMs);
                return GatewayAuthResult.success(referenceCode, false, "Grandstream GWN7001 cấp quyền Internet thành công", latencyMs);
            } else {
                log.warn("[GWN Cloud Gateway] Authorization returned HTTP {}: {}", response.statusCode(), response.body());
                failedOperations.incrementAndGet();
                saveTransaction(sessionId, "AUTHORIZE", false, response.statusCode(), "GWN Cloud từ chối: " + response.statusCode(), response.body(), latencyMs);
                return GatewayAuthResult.failure("GWN Cloud từ chối cấp quyền: " + response.statusCode(), response.statusCode(), false, latencyMs);
            }
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            failedOperations.incrementAndGet();
            log.error("[GWN Cloud Gateway] Error communicating with GWN Cloud API: {}", e.getMessage(), e);
            saveTransaction(sessionId, "AUTHORIZE", false, 500, e.getMessage(), null, latencyMs);
            return GatewayAuthResult.failure("Lỗi kết nối trạm phát GWN Cloud: " + e.getMessage(), 500, false, latencyMs);
        }
    }

    @Override
    public GatewayRevokeResult revokeSession(
            Long sessionId,
            String macAddress,
            String ipAddress,
            String gatewayReference
    ) {
        long startTime = System.currentTimeMillis();
        totalRevocations.incrementAndGet();

        if (!isLiveCredentialsConfigured()) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.info("[GWN Cloud Gateway] Revoked session (STANDBY MODE): session={}, MAC={}, Ref={}",
                    sessionId, macAddress, gatewayReference);
            saveTransaction(sessionId, "REVOKE", true, 200, null,
                    String.format("{\"gatewayRef\":\"%s\",\"status\":\"STANDBY_REVOKED\",\"mac\":\"%s\"}", gatewayReference, macAddress),
                    latencyMs);
            return GatewayRevokeResult.success(true, "Đã thu hồi quyền kết nối (GWN7001 Standby)", latencyMs);
        }

        try {
            String token = obtainGwnCloudToken();
            Map<String, Object> payload = new HashMap<>();
            payload.put("mac", formatMacAddress(macAddress));
            if (StringUtils.hasText(networkId)) {
                payload.put("networkId", networkId);
            }

            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(gwnBaseUrl.replaceAll("/+$", "") + "/api/v1.0/guest/revoke"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - startTime;

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            saveTransaction(sessionId, "REVOKE", success, response.statusCode(), success ? null : response.body(), response.body(), latencyMs);
            if (success) {
                return GatewayRevokeResult.success(false, "GWN Cloud thu hồi quyền MAC: " + macAddress, latencyMs);
            } else {
                return GatewayRevokeResult.failure("GWN Cloud từ chối thu hồi: " + response.statusCode(), response.statusCode(), false, latencyMs);
            }
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.warn("[GWN Cloud Gateway] Exception revoking session: {}", e.getMessage());
            saveTransaction(sessionId, "REVOKE", false, 500, e.getMessage(), null, latencyMs);
            return GatewayRevokeResult.failure("Lỗi thu hồi quyền mạng GWN Cloud: " + e.getMessage(), 500, false, latencyMs);
        }
    }

    @Override
    public GatewayStatusDto getStatus() {
        GatewayStatusDto dto = new GatewayStatusDto();
        dto.setAdapterType("GWN_CLOUD");
        dto.setHealthy(true);
        dto.setSimulated(!isLiveCredentialsConfigured());
        dto.setVersion("Grandstream GWN Cloud Open API v1.0 (GWN7001 Ready)");
        dto.setTotalAuthorizations(totalAuthorizations.get());
        dto.setTotalRevocations(totalRevocations.get());
        dto.setFailedOperations(failedOperations.get());

        if (isLiveCredentialsConfigured()) {
            dto.setStatusMessage("Đã cấu hình xác thực GWN Cloud. Sẵn sàng kích hoạt lệnh phần cứng trên router GWN7001.");
        } else {
            dto.setStatusMessage("Chế độ Sẵn sàng kết nối Grandstream GWN7001. Chỉ cần điền App ID & Secret Key để đồng bộ thực tế.");
        }
        return dto;
    }

    @Override
    public GatewayStatusDto updateConfig(MockGatewayConfigDto config) {
        log.info("[GWN Cloud Gateway] Dynamic configuration update requested");
        return getStatus();
    }

    private boolean isLiveCredentialsConfigured() {
        return StringUtils.hasText(appId) &&
               StringUtils.hasText(appSecret) &&
               !appId.equalsIgnoreCase("GWN_APP_ID") &&
               !appId.equalsIgnoreCase("YOUR_APP_ID");
    }

    private synchronized String obtainGwnCloudToken() throws Exception {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }

        Map<String, String> body = new HashMap<>();
        body.put("appId", appId.trim());
        body.put("secretKey", appSecret.trim());

        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(gwnBaseUrl.replaceAll("/+$", "") + "/api/v1.0/oauth/token"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode tokenNode = root.at("/data/token");
            if (!tokenNode.isMissingNode() && StringUtils.hasText(tokenNode.asText())) {
                cachedToken = tokenNode.asText();
                long expireSeconds = root.at("/data/expireTime").asLong(7200);
                tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expireSeconds - 300));
                return cachedToken;
            }
        }
        throw new IllegalStateException("Không thể lấy Token xác thực từ GWN Cloud (HTTP " + resp.statusCode() + "): " + resp.body());
    }

    private String formatMacAddress(String mac) {
        if (mac == null) return "00:00:00:00:00:00";
        String clean = mac.replaceAll("[^a-fA-F0-9]", "").toUpperCase();
        if (clean.length() == 12) {
            return String.format("%s:%s:%s:%s:%s:%s",
                    clean.substring(0, 2), clean.substring(2, 4), clean.substring(4, 6),
                    clean.substring(6, 8), clean.substring(8, 10), clean.substring(10, 12));
        }
        return mac.toUpperCase();
    }

    private void saveTransaction(Long sessionId, String operation, boolean success, Integer statusCode, String errorMsg, String rawResp, long elapsedMs) {
        try {
            GatewayTransaction tx = new GatewayTransaction(
                    sessionId,
                    operation,
                    "GWN_CLOUD",
                    !isLiveCredentialsConfigured(),
                    success,
                    statusCode,
                    errorMsg,
                    rawResp,
                    elapsedMs
            );
            transactionRepository.save(tx);
        } catch (Exception e) {
            log.warn("Could not save GWN gateway transaction: {}", e.getMessage());
        }
    }
}

