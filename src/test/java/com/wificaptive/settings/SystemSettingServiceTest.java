package com.wificaptive.settings;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.settings.dto.SystemSettingDto;
import com.wificaptive.settings.dto.UpdateSystemSettingRequest;
import com.wificaptive.settings.entity.SystemSetting;
import com.wificaptive.settings.repository.SystemSettingRepository;
import com.wificaptive.settings.service.SystemSettingService;
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
class SystemSettingServiceTest {

    @Mock
    private SystemSettingRepository repository;

    @Mock
    private AuditService auditService;

    private SystemSettingService service;

    @BeforeEach
    void setUp() {
        service = new SystemSettingService(repository, auditService);
    }

    @Test
    void testGetAllSettings() {
        SystemSetting s1 = new SystemSetting("ntp_server", "vn.pool.ntp.org", "NTP Server", "NETWORK");
        s1.setId(1L);
        when(repository.findAll()).thenReturn(List.of(s1));

        List<SystemSettingDto> list = service.getAllSettings();

        assertEquals(1, list.size());
        assertEquals("ntp_server", list.get(0).getSettingKey());
    }

    @Test
    void testUpdateSetting_Success() {
        SystemSetting s1 = new SystemSetting("max_failed_logins", "5", "Max logins", "SECURITY");
        s1.setId(2L);
        when(repository.findBySettingKey("max_failed_logins")).thenReturn(Optional.of(s1));
        when(repository.save(any(SystemSetting.class))).thenAnswer(i -> i.getArgument(0));

        UpdateSystemSettingRequest req = new UpdateSystemSettingRequest();
        req.setSettingValue("3");
        req.setDescription("Updated max logins");

        SystemSettingDto dto = service.updateSetting("max_failed_logins", req, "superadmin");

        assertNotNull(dto);
        assertEquals("3", dto.getSettingValue());
        verify(repository).save(any(SystemSetting.class));
    }
}
