package com.wificaptive.devices.scheduler;

import com.wificaptive.common.lock.DistributedLockService;
import com.wificaptive.devices.dto.DeviceSyncSummaryDto;
import com.wificaptive.devices.service.NetworkDeviceService;
import com.wificaptive.incidents.service.IncidentTicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.gateway.gwn-cloud.sync-enabled", havingValue = "true", matchIfMissing = true)
public class DeviceSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeviceSyncScheduler.class);

    private final NetworkDeviceService deviceService;
    private final IncidentTicketService incidentTicketService;
    private final DistributedLockService lockService;

    public DeviceSyncScheduler(NetworkDeviceService deviceService,
                               IncidentTicketService incidentTicketService,
                               DistributedLockService lockService) {
        this.deviceService = deviceService;
        this.incidentTicketService = incidentTicketService;
        this.lockService = lockService;
    }

    public DeviceSyncScheduler(NetworkDeviceService deviceService, IncidentTicketService incidentTicketService) {
        this(deviceService, incidentTicketService, new DistributedLockService(null));
    }

    /**
     * Periodically syncs hardware router/AP statuses from Grandstream GDMS / GWN Cloud in the background.
     * Default frequency: Every 5 minutes (300,000 ms), with initial delay of 30 seconds after startup.
     * Wrapped in a distributed lock so only one backend pod executes the cluster-wide sync.
     */
    @Scheduled(fixedRateString = "${app.gateway.gwn-cloud.sync-interval-ms:300000}", initialDelay = 30000)
    public void syncDevicesPeriodically() {
        lockService.runWithLock("scheduler:device-sync", Duration.ofMinutes(4), () -> {
            try {
                log.debug("[DeviceSyncScheduler] Starting background Grandstream device status sync...");
                DeviceSyncSummaryDto summary = deviceService.syncFromGrandstream("scheduler");
                log.info("[DeviceSyncScheduler] Background sync completed: {} devices synced, {} updated, {} created",
                        summary.getSyncedCount(), summary.getUpdatedCount(), summary.getCreatedCount());

                int autoClosed = incidentTicketService.autoResolveRecoveredIncidents();
                if (autoClosed > 0) {
                    log.info("Tự động đóng {} phiếu sự cố do thiết bị đã khôi phục kết nối ONLINE thành công", autoClosed);
                }
            } catch (Exception e) {
                log.warn("[DeviceSyncScheduler] Background sync encountered an issue: {}", e.getMessage());
            }
        });
    }
}

