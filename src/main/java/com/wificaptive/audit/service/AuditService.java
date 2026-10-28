package com.wificaptive.audit.service;

import com.wificaptive.audit.dto.AuditEventDto;
import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.entity.AuditEvent;
import com.wificaptive.audit.repository.AuditEventRepository;
import com.wificaptive.common.dto.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(AuditAction action, String actorUsername, String clientIp, String targetType, String targetId, String details) {
        try {
            AuditEvent event = new AuditEvent(action, actorUsername, clientIp, targetType, targetId, details);
            auditEventRepository.save(event);
            log.debug("Audit recorded: action={}, actor={}, target={}:{}", action, actorUsername, targetType, targetId);
        } catch (Exception ex) {
            log.error("Failed to record audit event: {}", ex.getMessage(), ex);
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
