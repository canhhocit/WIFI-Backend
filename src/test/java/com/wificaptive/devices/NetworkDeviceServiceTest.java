package com.wificaptive.devices;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.devices.dto.DeviceSwapRequest;
import com.wificaptive.devices.dto.NetworkDeviceDto;
import com.wificaptive.devices.entity.DeviceSwapLog;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.DeviceSwapLogRepository;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.devices.service.NetworkDeviceService;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkDeviceServiceTest {

    @Mock
    private NetworkDeviceRepository deviceRepository;

    @Mock
    private DeviceSwapLogRepository swapLogRepository;

    @Mock
    private WifiLocationRepository locationRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private NetworkDeviceService deviceService;

    private WifiLocation location;
    private NetworkDevice oldDevice;

    @BeforeEach
    void setUp() {
        location = new WifiLocation();
        location.setId(1L);
        location.setName("NVH Cát Bà");

        oldDevice = new NetworkDevice();
        oldDevice.setId(10L);
        oldDevice.setDeviceType("ROUTER");
        oldDevice.setSerialNumber("SN-OLD-001");
        oldDevice.setMacAddress("00:1A:2B:3C:4D:5E");
        oldDevice.setStatus("ONLINE");
        oldDevice.setLocation(location);
    }

    @Test
    void testSwapDevice_success() {
        DeviceSwapRequest req = new DeviceSwapRequest();
        req.setLocationId(1L);
        req.setOldDeviceId(10L);
        req.setDeviceType("ROUTER");
        req.setNewSerialNumber("SN-NEW-999");
        req.setNewMacAddress("00:1A:2B:3C:4D:99");
        req.setNewModel("RB750Gr3");
        req.setNewVendor("Mikrotik");
        req.setSwapReason("Cháy cổng WAN do sét đánh");

        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(oldDevice));
        when(deviceRepository.existsByMacAddress("00:1A:2B:3C:4D:99")).thenReturn(false);
        when(deviceRepository.existsBySerialNumber("SN-NEW-999")).thenReturn(false);
        when(deviceRepository.save(any(NetworkDevice.class))).thenAnswer(inv -> {
            NetworkDevice dev = inv.getArgument(0);
            if (dev.getId() == null) dev.setId(20L);
            return dev;
        });

        NetworkDeviceDto result = deviceService.swapDevice(req, "tech_admin");

        assertThat(result).isNotNull();
        assertThat(result.getSerialNumber()).isEqualTo("SN-NEW-999");
        assertThat(result.getMacAddress()).isEqualTo("00:1A:2B:3C:4D:99");
        assertThat(oldDevice.getStatus()).isEqualTo("DECOMMISSIONED");

        verify(swapLogRepository).save(any(DeviceSwapLog.class));
        verify(auditService).recordEvent(any(), any(), any(), any(), any(), any());
    }
}

