package com.wificaptive.portal.scheduler;

import com.wificaptive.incidents.entity.IncidentTicket;
import com.wificaptive.incidents.entity.IncidentUpdate;
import com.wificaptive.incidents.repository.IncidentTicketRepository;
import com.wificaptive.incidents.repository.IncidentUpdateRepository;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Giám sát chủ động trạng thái sẵn sàng của trang chào (Captive Portal Probing).
 * Tự động thăm dò (probe) HTTP GET trang chào định kỳ mỗi 5 phút.
 * Nếu độ trễ phản hồi > 3.000ms hoặc gặp mã lỗi HTTP (5xx, timeout, unreachable)
 * hệ thống sẽ tự động phát hiện và khởi tạo phiếu sự cố kỹ thuật.
 */
@Component
@ConditionalOnProperty(name = "app.portal.health-probe.enabled", havingValue = "true", matchIfMissing = true)
public class PortalHealthProbeScheduler {

    private static final Logger log = LoggerFactory.getLogger(PortalHealthProbeScheduler.class);
    private static final AtomicInteger COUNTER = new AtomicInteger(500);

    private final WifiLocationRepository locationRepository;
    private final IncidentTicketRepository ticketRepository;
    private final IncidentUpdateRepository updateRepository;
    private final RestTemplate restTemplate;

    @Value("${server.port:8080}")
    private int serverPort;

    public PortalHealthProbeScheduler(
            WifiLocationRepository locationRepository,
            IncidentTicketRepository ticketRepository,
            IncidentUpdateRepository updateRepository,
            RestTemplateBuilder restTemplateBuilder) {
        this.locationRepository = locationRepository;
        this.ticketRepository = ticketRepository;
        this.updateRepository = updateRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(4))
                .build();
    }

    /**
     * Chu kỳ kiểm tra chủ động: Mỗi 5 phút (300.000 ms), bắt đầu sau khi hệ thống khởi động 60 giây.
     */
    @Scheduled(fixedRateString = "${app.portal.health-probe.interval-ms:300000}", initialDelay = 60000)
    public void probePortalHealth() {
        List<WifiLocation> locations = locationRepository.findAll();
        if (locations.isEmpty()) {
            return;
        }

        log.debug("[Portal Prober] Bắt đầu chu kỳ thăm dò chủ động độ khả dụng Cổng chào (Portal Prober)...");

        // Chọn các điểm active tiêu biểu hoặc điểm đầu tiên để probe sức khỏe cổng chào
        WifiLocation sampleLocation = locations.get(0);
        String probeUrl = String.format("http://localhost:%d/api/portal/init?location=%s", serverPort, sampleLocation.getSlug());

        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorMessage = null;
        long latencyMs = 0;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(probeUrl, String.class);
            latencyMs = System.currentTimeMillis() - startTime;
            if (response.getStatusCode().is2xxSuccessful()) {
                if (latencyMs > 3000) {
                    errorMessage = String.format("Cổng chào phản hồi chậm bất thường (Độ trễ: %d ms > ngưỡng cảnh báo 3.000 ms)", latencyMs);
                } else {
                    success = true;
                    log.debug("[Portal Prober] Cổng chào hoạt động tốt, độ trễ: {} ms", latencyMs);
                }
            } else {
                errorMessage = String.format("Cổng chào trả mã lỗi HTTP: %s, độ trễ: %d ms", response.getStatusCode(), latencyMs);
            }
        } catch (Exception e) {
            latencyMs = System.currentTimeMillis() - startTime;
            errorMessage = String.format("Không thể kết nối cổng chào (Lỗi: %s, độ trễ: %d ms)", e.getMessage(), latencyMs);
        }

        if (!success) {
            log.warn("[Portal Prober] Cảnh báo suy giảm chất lượng cổng chào: {}", errorMessage);
            handleDegradedPortal(sampleLocation, errorMessage, latencyMs);
        }
    }

    private void handleDegradedPortal(WifiLocation location, String issueDetails, long latencyMs) {
        // Kiểm tra xem đã có phiếu sự cố dạng PORTAL_DEGRADED đang mở chưa
        List<IncidentTicket> openTickets = ticketRepository.findByLocationIdAndStatusIn(
                location.getId(), List.of("NEW", "ASSIGNED", "IN_PROGRESS"));

        boolean hasActiveProbeTicket = openTickets.stream()
                .anyMatch(t -> "PORTAL_DEGRADED".equalsIgnoreCase(t.getIncidentType()));

        if (!hasActiveProbeTicket) {
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String ticketCode = String.format("SC-%s-%04d", datePart, COUNTER.incrementAndGet());

            IncidentTicket ticket = new IncidentTicket();
            ticket.setTicketCode(ticketCode);
            ticket.setLocationId(location.getId());
            ticket.setIncidentType("PORTAL_DEGRADED");
            ticket.setSeverity("MAJOR");
            ticket.setStatus("NEW");
            ticket.setTitle("Hệ thống phát hiện suy giảm chất lượng Cổng chào tại " + location.getName());
            ticket.setDescription(String.format("Hệ thống giám sát chủ động (Portal Prober) ghi nhận sự cố: %s. Điểm phát: %s. Cần kiểm tra dịch vụ Portal & Gateway.",
                    issueDetails, location.getName()));
            ticket.setSlaDeadline(Instant.now().plus(4, ChronoUnit.HOURS));

            IncidentTicket saved = ticketRepository.save(ticket);

            updateRepository.save(new IncidentUpdate(
                    saved.getId(),
                    "portal_health_prober",
                    "AUTO_TRIGGER",
                    "Tự động tạo phiếu sự cố kỹ thuật từ bộ quét chủ động độ trễ & khả dụng Cổng chào (Độ trễ: " + latencyMs + "ms)."
            ));

            log.warn("[Portal Prober] Đã tự động tạo phiếu sự cố {} do Cổng chào suy giảm chất lượng", ticketCode);
        }
    }
}

