package com.wificaptive.devices.scheduler;

import com.wificaptive.devices.dto.DeviceSyncSummaryDto;
import com.wificaptive.devices.service.NetworkDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.gateway.gwn-cloud.sync-enabled", havingValue = "true", matchIfMissing = true)
public class DeviceSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeviceSyncScheduler.class);

    private final NetworkDeviceService deviceService;

    public DeviceSyncScheduler(NetworkDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * Periodically syncs hardware router/AP statuses from Grandstream GDMS / GWN Cloud in the background.
     * Default frequency: Every 5 minutes (300,000 ms), with initial delay of 30 seconds after startup.
     */
    @Scheduled(fixedRateString = "${app.gateway.gwn-cloud.sync-interval-ms:300000}", initialDelay = 30000)
    public void syncDevicesPeriodically() {
        try {
            log.debug("[DeviceSyncScheduler] Starting background Grandstream device status sync...");
            DeviceSyncSummaryDto summary = deviceService.syncFromGrandstream("scheduler");
            log.info("[DeviceSyncScheduler] Background sync completed: {} devices synced, {} updated, {} created",
                    summary.getSyncedCount(), summary.getUpdatedCount(), summary.getCreatedCount());
        } catch (Exception e) {
            log.warn("[DeviceSyncScheduler] Background sync encountered an issue: {}", e.getMessage());
        }
    }
}

