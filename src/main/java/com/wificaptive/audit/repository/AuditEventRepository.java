package com.wificaptive.audit.repository;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AuditEvent> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);
}
