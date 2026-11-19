package com.wificaptive.audit.service;

import com.wificaptive.audit.dto.AuditEventDto;
import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.entity.AuditEvent;
import com.wificaptive.audit.repository.AuditEventRepository;
import com.wificaptive.common.dto.PageResponse;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final int BATCH_SIZE = 200;

    private final AuditEventRepository auditEventRepository;
    private final BlockingQueue<AuditEvent> auditQueue = new LinkedBlockingQueue<>(50000);

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * ĐỀ XUẤT 2: Ghi nhận sự kiện kiểm toán bất đồng bộ (Non-blocking In-Memory Queue).
     * Giải phóng hoàn toàn luồng người dùng và kết nối Hikari connection pool ngay lập tức (< 0.01ms).
     */
    public void recordEvent(AuditAction action, String actorUsername, String clientIp, String targetType, String targetId, String details) {
        try {
            AuditEvent event = new AuditEvent(action, actorUsername, clientIp, targetType, targetId, details);
            boolean offered = auditQueue.offer(event);
            if (!offered) {
                log.warn("[AuditService] Audit queue full (50,000 items). Dropping event to protect latency: {}", action);
            }
        } catch (Exception ex) {
            log.error("[AuditService] Failed to enqueue audit event: {}", ex.getMessage());
        }
    }

    /**
     * Tiến trình nền định kỳ xả hàng đợi ghi CSDL theo mẻ (Batch Insert 200 bản ghi/lệnh).
     * Tiết kiệm 99% I/O đĩa và giải phóng kết nối CSDL.
     */
    @Scheduled(fixedDelay = 1000)
    public void flushAuditQueue() {
        if (auditQueue.isEmpty()) {
            return;
        }

        List<AuditEvent> batch = new ArrayList<>(BATCH_SIZE);
        auditQueue.drainTo(batch, BATCH_SIZE);

        if (!batch.isEmpty()) {
            try {
                auditEventRepository.saveAll(batch);
                log.debug("[AuditService] Successfully flushed {} audit events in batch to database", batch.size());
            } catch (Exception ex) {
                log.error("[AuditService] Error saving batch audit events: {}", ex.getMessage(), ex);
            }
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("[AuditService] Application shutting down, flushing remaining {} audit events...", auditQueue.size());
        List<AuditEvent> remaining = new ArrayList<>();
        auditQueue.drainTo(remaining);
        if (!remaining.isEmpty()) {
            try {
                auditEventRepository.saveAll(remaining);
            } catch (Exception ex) {
                log.error("[AuditService] Failed to flush audit events on shutdown: {}", ex.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDto> getAuditEvents(String actionStr, Pageable pageable) {
        if (actionStr == null || actionStr.isBlank()) {
            return getAuditEvents((AuditAction) null, pageable);
        }

        String normalized = actionStr.trim().toUpperCase();
        // Check legacy aliases
        if ("USER_CREATE".equals(normalized)) {
            return getAuditEvents(AuditAction.ADMIN_CREATE_USER, pageable);
        } else if ("USER_UPDATE".equals(normalized)) {
            return getAuditEvents(AuditAction.ADMIN_UPDATE_USER, pageable);
        } else if ("USER_UNLOCK".equals(normalized)) {
            return getAuditEvents(AuditAction.ADMIN_UNLOCK_USER, pageable);
        }

        try {
            AuditAction action = AuditAction.valueOf(normalized);
            return getAuditEvents(action, pageable);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown audit action requested: {}", actionStr);
            return PageResponse.from(Page.empty(pageable));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDto> getAuditEvents(AuditAction action, Pageable pageable) {
        Page<AuditEvent> page = (action != null)
                ? auditEventRepository.findByActionOrderByCreatedAtDesc(action, pageable)
                : auditEventRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page.map(AuditEventDto::from));
    }
}
