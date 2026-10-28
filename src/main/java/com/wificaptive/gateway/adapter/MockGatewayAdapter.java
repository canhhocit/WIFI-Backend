package com.wificaptive.gateway.adapter;

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

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component("mockGatewayAdapter")
public class MockGatewayAdapter implements GatewayAdapter {

    private static final Logger log = LoggerFactory.getLogger(MockGatewayAdapter.class);

    private final GatewayTransactionRepository transactionRepository;

    private volatile boolean simulateFailures;
    private volatile int simulateLatencyMs;

    private final AtomicLong totalAuthorizations = new AtomicLong(0);
    private final AtomicLong totalRevocations = new AtomicLong(0);
    private final AtomicLong failedOperations = new AtomicLong(0);

    public MockGatewayAdapter(
            GatewayTransactionRepository transactionRepository,
            @Value("${app.gateway.simulate-failures:false}") boolean simulateFailures,
            @Value("${app.gateway.simulate-latency-ms:100}") int simulateLatencyMs) {
        this.transactionRepository = transactionRepository;
        this.simulateFailures = simulateFailures;
        this.simulateLatencyMs = simulateLatencyMs;
    }

    @Override
    public GatewayAuthResult authorizeSession(Long sessionId, String macAddress, String ipAddress, Integer durationMinutes, Integer maxBandwidthMbps) {
        long startTime = System.currentTimeMillis();
        applySimulatedLatency();

        long elapsed = System.currentTimeMillis() - startTime;

        if (simulateFailures) {
            failedOperations.incrementAndGet();
            String errorMsg = "Simulated Gateway Failure: Mock gateway failed to apply firewall pass rule for MAC " + macAddress;
            log.warn("[MOCK GATEWAY] Authorization simulated failure: session={}, mac={}, ip={}", sessionId, macAddress, ipAddress);

            saveTransaction(sessionId, "AUTHORIZE", false, 502, errorMsg, "{\"error\": \"SIMULATED_GATEWAY_REJECTION\"}", elapsed);
            return GatewayAuthResult.failure(errorMsg, 502, true, elapsed);
        }

        totalAuthorizations.incrementAndGet();
        String gatewayRef = "MOCK-GW-AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[MOCK GATEWAY - SIMULATION ONLY] Session {} AUTHORIZED on gateway. MAC: {}, IP: {}, Duration: {}m, Bandwidth: {}Mbps. Reference: {}. (NOTE: Simulated only; no physical firewall was altered).",
                sessionId, macAddress, ipAddress, durationMinutes, maxBandwidthMbps, gatewayRef);

        saveTransaction(sessionId, "AUTHORIZE", true, 200, null,
                String.format("{\"gatewayRef\":\"%s\",\"status\":\"SIMULATED_PASS\",\"mac\":\"%s\",\"ip\":\"%s\"}", gatewayRef, macAddress, ipAddress),
                elapsed);

        return GatewayAuthResult.success(
                gatewayRef,
                true,
                "Simulated authorization granted on Mock Gateway (Hardware enforcement not active)",
                elapsed
        );
    }

    @Override
    public GatewayRevokeResult revokeSession(Long sessionId, String macAddress, String ipAddress, String gatewayReference) {
        long startTime = System.currentTimeMillis();
        applySimulatedLatency();

        long elapsed = System.currentTimeMillis() - startTime;

        if (simulateFailures) {
            failedOperations.incrementAndGet();
            String errorMsg = "Simulated Gateway Failure: Mock gateway failed to revoke firewall rule for ref " + gatewayReference;
            log.warn("[MOCK GATEWAY] Revocation simulated failure: session={}, ref={}", sessionId, gatewayReference);

            saveTransaction(sessionId, "REVOKE", false, 502, errorMsg, "{\"error\": \"SIMULATED_REVOCATION_REJECTION\"}", elapsed);
            return GatewayRevokeResult.failure(errorMsg, 502, true, elapsed);
        }

        totalRevocations.incrementAndGet();
        log.info("[MOCK GATEWAY - SIMULATION ONLY] Session {} REVOKED on gateway. Reference: {}, MAC: {}. (Simulated firewall rule removed).",
                sessionId, gatewayReference, macAddress);

        saveTransaction(sessionId, "REVOKE", true, 200, null,
                String.format("{\"gatewayRef\":\"%s\",\"status\":\"SIMULATED_BLOCKED\"}", gatewayReference),
                elapsed);

        return GatewayRevokeResult.success(
                true,
                "Simulated revocation applied on Mock Gateway",
                elapsed
        );
    }

    @Override
    public GatewayStatusDto getStatus() {
        GatewayStatusDto dto = new GatewayStatusDto();
        dto.setAdapterType("MOCK");
        dto.setHealthy(!simulateFailures);
        dto.setSimulated(true);
        dto.setVersion("v1.0-mock");
        dto.setStatusMessage(simulateFailures
                ? "Simulated Gateway Failure mode active (All incoming gateway operations will fail)"
                : "Mock Gateway operational (Simulation mode ready for OPNsense integration)");
        dto.setTotalAuthorizations(totalAuthorizations.get());
        dto.setTotalRevocations(totalRevocations.get());
        dto.setFailedOperations(failedOperations.get());
        dto.setSimulateFailures(simulateFailures);
        dto.setSimulatedLatencyMs(simulateLatencyMs);
        dto.setLastCheckedAt(Instant.now());
        return dto;
    }

    @Override
    public GatewayStatusDto updateConfig(MockGatewayConfigDto config) {
        if (config.getSimulateFailures() != null) {
            this.simulateFailures = config.getSimulateFailures();
            log.info("[MOCK GATEWAY] Updated simulateFailures={}", this.simulateFailures);
        }
        if (config.getSimulatedLatencyMs() != null) {
            this.simulateLatencyMs = config.getSimulatedLatencyMs();
            log.info("[MOCK GATEWAY] Updated simulateLatencyMs={}", this.simulateLatencyMs);
        }
        return getStatus();
    }

    private void applySimulatedLatency() {
        if (simulateLatencyMs > 0) {
            try {
                Thread.sleep(simulateLatencyMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void saveTransaction(Long sessionId, String operation, boolean success, Integer statusCode, String errorMsg, String rawResp, long elapsedMs) {
        try {
            GatewayTransaction tx = new GatewayTransaction(
                    sessionId,
                    operation,
                    "MOCK",
                    true,
                    success,
                    statusCode,
                    errorMsg,
                    rawResp,
                    elapsedMs
            );
            transactionRepository.save(tx);
        } catch (Exception e) {
            log.error("Could not persist gateway transaction audit record: {}", e.getMessage());
        }
    }
}
