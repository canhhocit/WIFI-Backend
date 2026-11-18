package com.wificaptive.audit;

import com.wificaptive.audit.dto.AuditEventDto;
import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.entity.AuditEvent;
import com.wificaptive.audit.repository.AuditEventRepository;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditEventRepository);
    }

    @Test
    void testGetAuditEventsWithDeviceUnblock() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditEvent event = new AuditEvent(AuditAction.DEVICE_UNBLOCK, "admin", "127.0.0.1", "DEVICE", "00:11:22:33:44:55", "Unblocked device MAC");
        when(auditEventRepository.findByActionOrderByCreatedAtDesc(eq(AuditAction.DEVICE_UNBLOCK), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), pageable, 1));

        PageResponse<AuditEventDto> result = auditService.getAuditEvents("DEVICE_UNBLOCK", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(AuditAction.DEVICE_UNBLOCK, result.getContent().get(0).getAction());
        verify(auditEventRepository).findByActionOrderByCreatedAtDesc(eq(AuditAction.DEVICE_UNBLOCK), any(Pageable.class));
    }

    @Test
    void testGetAuditEventsWithUserCreateAlias() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditEvent event = new AuditEvent(AuditAction.ADMIN_CREATE_USER, "admin", "127.0.0.1", "USER", "1", "Created user");
        when(auditEventRepository.findByActionOrderByCreatedAtDesc(eq(AuditAction.ADMIN_CREATE_USER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), pageable, 1));

        PageResponse<AuditEventDto> result = auditService.getAuditEvents("USER_CREATE", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(auditEventRepository).findByActionOrderByCreatedAtDesc(eq(AuditAction.ADMIN_CREATE_USER), any(Pageable.class));
    }

    @Test
    void testGetAuditEventsWithUnknownActionReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<AuditEventDto> result = auditService.getAuditEvents("NON_EXISTENT_ACTION", pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(auditEventRepository, never()).findByActionOrderByCreatedAtDesc(any(), any());
    }
}
