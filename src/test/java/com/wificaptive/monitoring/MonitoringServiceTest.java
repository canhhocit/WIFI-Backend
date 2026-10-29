package com.wificaptive.monitoring;

import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.monitoring.dto.TelemetryIngestRequest;
import com.wificaptive.monitoring.dto.TelemetryMetricDto;
import com.wificaptive.monitoring.entity.AlertRule;
import com.wificaptive.monitoring.entity.DeviceTelemetryMetric;
import com.wificaptive.monitoring.repository.AlertRuleRepository;
import com.wificaptive.monitoring.repository.DeviceTelemetryMetricRepository;
import com.wificaptive.monitoring.service.AlertNotificationDispatcher;
import com.wificaptive.monitoring.service.MonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private DeviceTelemetryMetricRepository metricRepository;

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private NetworkDeviceRepository deviceRepository;

    @Mock
    private AlertNotificationDispatcher dispatcher;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        monitoringService = new MonitoringService(
                metricRepository,
                alertRuleRepository,
                deviceRepository,
                dispatcher
        );
    }

    @Test
    void testRecordTelemetryTriggersAlert() {
        NetworkDevice dev = new NetworkDevice();
        dev.setId(10L);
        dev.setMacAddress("00:11:22:33:44:55");
        com.wificaptive.locations.entity.WifiLocation loc = new com.wificaptive.locations.entity.WifiLocation();
        loc.setId(1L);
        dev.setLocation(loc);

        when(deviceRepository.findById(10L)).thenReturn(Optional.of(dev));

        DeviceTelemetryMetric metric = new DeviceTelemetryMetric();
        metric.setDeviceId(10L);
        metric.setLocationId(1L);
        metric.setCpuUsagePercent(92.5); // High CPU

        when(metricRepository.save(any(DeviceTelemetryMetric.class))).thenReturn(metric);

        AlertRule rule = new AlertRule();
        rule.setRuleName("CPU Warning");
        rule.setMetricType("HIGH_CPU");
        rule.setThresholdValue(85.0);
        rule.setNotificationChannel("TELEGRAM");
        rule.setEnabled(true);

        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        TelemetryIngestRequest req = new TelemetryIngestRequest();
        req.setDeviceId(10L);
        req.setLocationId(1L);
        req.setCpuUsagePercent(92.5);

        TelemetryMetricDto res = monitoringService.recordTelemetry(req);
        assertNotNull(res);
        verify(dispatcher, times(1)).dispatch(any(), any(), any());
    }

    @Test
    void testAnalyzeRootCausePowerOutageVsFiberCut() {
        NetworkDevice ont = new NetworkDevice();
        ont.setDeviceType("ONT");
        ont.setStatus("OFFLINE");

        NetworkDevice ap = new NetworkDevice();
        ap.setDeviceType("ACCESS_POINT");
        ap.setStatus("ONLINE");

        when(deviceRepository.findByLocationId(1L)).thenReturn(List.of(ont, ap));

        String cause = monitoringService.analyzeRootCause(1L);
        assertEquals("FIBER_CUT", cause);
    }
}

