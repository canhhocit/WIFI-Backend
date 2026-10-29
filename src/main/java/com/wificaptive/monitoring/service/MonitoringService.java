package com.wificaptive.monitoring.service;

import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.monitoring.dto.AlertRuleDto;
import com.wificaptive.monitoring.dto.TelemetryIngestRequest;
import com.wificaptive.monitoring.dto.TelemetryMetricDto;
import com.wificaptive.monitoring.entity.AlertRule;
import com.wificaptive.monitoring.entity.DeviceTelemetryMetric;
import com.wificaptive.monitoring.repository.AlertRuleRepository;
import com.wificaptive.monitoring.repository.DeviceTelemetryMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final DeviceTelemetryMetricRepository metricRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final NetworkDeviceRepository deviceRepository;
    private final AlertNotificationDispatcher dispatcher;

    public MonitoringService(
            DeviceTelemetryMetricRepository metricRepository,
            AlertRuleRepository alertRuleRepository,
            NetworkDeviceRepository deviceRepository,
            AlertNotificationDispatcher dispatcher) {
        this.metricRepository = metricRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.deviceRepository = deviceRepository;
        this.dispatcher = dispatcher;
    }

    @Transactional
    public TelemetryMetricDto recordTelemetry(TelemetryIngestRequest req) {
        NetworkDevice device = deviceRepository.findById(req.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice not found with id: " + req.getDeviceId()));

        DeviceTelemetryMetric m = new DeviceTelemetryMetric();
        m.setDeviceId(req.getDeviceId());
        m.setLocationId(req.getLocationId() != null ? req.getLocationId() : device.getLocationId());
        m.setCpuUsagePercent(req.getCpuUsagePercent());
        m.setMemoryUsagePercent(req.getMemoryUsagePercent());
        m.setActiveClientCount(req.getActiveClientCount());
        m.setRxBytesSec(req.getRxBytesSec());
        m.setTxBytesSec(req.getTxBytesSec());
        m.setPingLatencyMs(req.getPingLatencyMs());
        m.setPacketLossPercent(req.getPacketLossPercent());
        m.setRecordedAt(Instant.now());

        DeviceTelemetryMetric saved = metricRepository.save(m);

        // Update device status and last seen
        device.setLastSeenAt(m.getRecordedAt());
        if (m.getPacketLossPercent() != null && m.getPacketLossPercent() >= 100.0) {
            device.setStatus("OFFLINE");
        } else {
            device.setStatus("ONLINE");
        }
        deviceRepository.save(device);

        // Evaluate against active alert rules
        evaluateAlertRules(saved, device);

        return TelemetryMetricDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TelemetryMetricDto> getLatestMetricsForLocation(Long locationId, int limit) {
        return metricRepository.findLatestByLocationId(locationId, PageRequest.of(0, limit))
                .stream().map(TelemetryMetricDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertRuleDto> getAllAlertRules() {
        return alertRuleRepository.findAll().stream().map(AlertRuleDto::from).collect(Collectors.toList());
    }

    @Transactional
    public AlertRuleDto updateAlertRuleStatus(Long id, boolean enabled) {
        AlertRule r = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule not found with id: " + id));
        r.setEnabled(enabled);
        return AlertRuleDto.from(alertRuleRepository.save(r));
    }

    /**
     * Phân loại nguyên nhân gốc rễ (Root Cause Analysis):
     * - Nếu toàn bộ thiết bị trạm (ONT + AP) cùng mất tín hiệu đồng thời -> POWER_OUTAGE (Mất điện lưới).
     * - Nếu ONT mất tín hiệu quang trong khi AP nội bộ vẫn ping được qua switch -> FIBER_CUT (Đứt cáp quang).
     */
    public String analyzeRootCause(Long locationId) {
        List<NetworkDevice> devices = deviceRepository.findByLocationId(locationId);
        if (devices.isEmpty()) return "HARDWARE_FAULT";

        long offlineCount = devices.stream().filter(d -> "OFFLINE".equalsIgnoreCase(d.getStatus())).count();
        if (offlineCount == devices.size()) {
            return "POWER_OUTAGE"; // Tất cả thiết bị cùng tắt nguồn -> mất điện trạm
        }

        boolean ontOffline = devices.stream().anyMatch(d -> "ONT".equalsIgnoreCase(d.getDeviceType()) && "OFFLINE".equalsIgnoreCase(d.getStatus()));
        boolean apOnline = devices.stream().anyMatch(d -> "ACCESS_POINT".equalsIgnoreCase(d.getDeviceType()) && "ONLINE".equalsIgnoreCase(d.getStatus()));

        if (ontOffline && apOnline) {
            return "FIBER_CUT"; // ONT mất kết nối quang ngoại vi trong khi AP nội bộ vẫn có nguồn
        }

        return "HARDWARE_FAULT";
    }

    private void evaluateAlertRules(DeviceTelemetryMetric metric, NetworkDevice device) {
        List<AlertRule> rules = alertRuleRepository.findByEnabledTrue();
        for (AlertRule rule : rules) {
            boolean triggered = false;
            String details = "";

            if ("HIGH_CPU".equalsIgnoreCase(rule.getMetricType()) && metric.getCpuUsagePercent() != null) {
                if (metric.getCpuUsagePercent() >= rule.getThresholdValue()) {
                    triggered = true;
                    details = String.format("CPU thiết bị %s đạt %.1f%% (Vượt ngưỡng %.1f%%)",
                            device.getMacAddress(), metric.getCpuUsagePercent(), rule.getThresholdValue());
                }
            } else if ("HIGH_PACKET_LOSS".equalsIgnoreCase(rule.getMetricType()) && metric.getPacketLossPercent() != null) {
                if (metric.getPacketLossPercent() >= rule.getThresholdValue()) {
                    triggered = true;
                    details = String.format("Tỷ lệ rớt gói mạng thiết bị %s đạt %.1f%% (Vượt ngưỡng %.1f%%)",
                            device.getMacAddress(), metric.getPacketLossPercent(), rule.getThresholdValue());
                }
            } else if ("OFFLINE".equalsIgnoreCase(rule.getMetricType())) {
                if ("OFFLINE".equalsIgnoreCase(device.getStatus())) {
                    triggered = true;
                    details = String.format("Mất kết nối thiết bị %s (Loại: %s) tại điểm phát sóng id: %d",
                            device.getMacAddress(), device.getDeviceType(), metric.getLocationId());
                }
            }

            if (triggered) {
                dispatcher.dispatch(rule, rule.getRuleName(), details);
            }
        }
    }
}

