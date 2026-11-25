package com.wificaptive.devices;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.devices.dto.DeviceDto;
import com.wificaptive.devices.entity.Device;
import com.wificaptive.devices.repository.DeviceRepository;
import com.wificaptive.devices.service.DeviceService;
import com.wificaptive.sessions.service.GuestSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private GuestSessionService guestSessionService;

    @Test
    void testToggleBlockDevice_EnforcesAndReleasesCloudBlock() {
        DeviceService service = new DeviceService(deviceRepository, auditService, guestSessionService);

        Device dev = new Device("aa:bb:cc:11:22:33", "192.168.80.10", "Client-2233", "Mozilla", "Android");
        dev.setId(5L);
        dev.setBlocked(false);

        when(deviceRepository.findById(5L)).thenReturn(Optional.of(dev));
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceDto blockedDto = service.toggleBlockDevice(5L, true, "admin");
        assertThat(blockedDto.isBlocked()).isTrue();
        verify(guestSessionService).enforceHardwareMacBlock(
                "aa:bb:cc:11:22:33", "admin", "Thiết bị bị chặn từ trang Quản lý thiết bị");

        DeviceDto unblockedDto = service.toggleBlockDevice(5L, false, "admin");
        assertThat(unblockedDto.isBlocked()).isFalse();
        verify(guestSessionService).releaseHardwareMacBlock("aa:bb:cc:11:22:33");
    }
}
