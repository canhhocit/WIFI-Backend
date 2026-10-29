package com.wificaptive.monitoring.service;

import com.wificaptive.monitoring.entity.AlertRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AlertNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationDispatcher.class);

    public void dispatch(AlertRule rule, String subject, String message) {
        String channel = rule.getNotificationChannel();
        String target = rule.getTargetChannelId() != null ? rule.getTargetChannelId() : "DEFAULT_CHANNEL";

        if ("TELEGRAM".equalsIgnoreCase(channel)) {
            log.warn("[ALERT DISPATCHER -> TELEGRAM BOT: {}] [{}] {}: {}",
                    target, rule.getSeverity(), subject, message);
        } else if ("WEBHOOK".equalsIgnoreCase(channel)) {
            log.warn("[ALERT DISPATCHER -> WEBHOOK: {}] [{}] {}: {}",
                    target, rule.getSeverity(), subject, message);
        } else {
            log.warn("[ALERT DISPATCHER -> {}] [{}] {}: {}",
                    channel, rule.getSeverity(), subject, message);
        }
    }
}

