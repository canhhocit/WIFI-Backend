package com.wificaptive.devices.mesh;

import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter;
import com.wificaptive.gateway.dto.GwnRouterPorts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MeshGwnStatusSchedulerTest {

    private GwnCloudGatewayAdapter gwn;
    private NetworkDeviceRepository repo;
    private MeshApMonitorService monitor;
    private NetworkDevice mesh;

    @BeforeEach
    void setUp() {
        gwn = mock(GwnCloudGatewayAdapter.class);
        repo = mock(NetworkDeviceRepository.class);
        monitor = mock(MeshApMonitorService.class);
        when(monitor.isAgentFed(any())).thenReturn(false);

        mesh = new NetworkDevice();
        mesh.setId(5L);
        mesh.setDeviceType("ACCESS_POINT");
        mesh.setModel("vAP-32X6V1");
        mesh.setVendor("Viettel High Tech");
        mesh.setMacAddress("24:0B:2B:47:2A:22");
        mesh.setStatus("ONLINE");
        when(repo.findAll()).thenReturn(List.of(mesh));

        // GWN: LAN4 down (mesh này đã rút), LAN5 & LAN6 up (2 mesh khác đang cắm)
        Map<String, Boolean> lan = new LinkedHashMap<>();
        lan.put("4", false);
        lan.put("5", true);
        lan.put("6", true);
        when(gwn.fetchRouterPorts()).thenReturn(new GwnRouterPorts(true, lan));
    }

    @Test
    void mappedMeshUsesItsOwnPort_offWhenThatPortDownEvenIfOtherLanUp() {
        // Khai cổng: mesh .111 ở LAN4 -> phải OFFLINE vì LAN4 down (dù LAN5/6 up)
        MeshGwnStatusScheduler s = new MeshGwnStatusScheduler(gwn, repo, monitor, "24:0B:2B:47:2A:22=4");
        s.refreshNow();
        verify(monitor).ingestGwnStatus(eq(mesh), eq(false), any());
        verify(gwn, never()).fetchClientInfoFromCloud(any());
    }

    @Test
    void mappedMeshOnlineWhenItsPortUp() {
        Map<String, Boolean> lan = new LinkedHashMap<>();
        lan.put("4", true);
        when(gwn.fetchRouterPorts()).thenReturn(new GwnRouterPorts(true, lan));
        MeshGwnStatusScheduler s = new MeshGwnStatusScheduler(gwn, repo, monitor, "24:0B:2B:47:2A:22=4");
        s.refreshNow();
        verify(monitor).ingestGwnStatus(eq(mesh), eq(true), any());
    }

    @Test
    void unmappedMeshFallsBackToAnyLanUp() {
        // Chưa khai cổng -> giả định 1 mesh/điểm: còn LAN5/6 up nên coi là ONLINE
        MeshGwnStatusScheduler s = new MeshGwnStatusScheduler(gwn, repo, monitor, "");
        s.refreshNow();
        verify(monitor).ingestGwnStatus(eq(mesh), eq(true), any());
    }

    @Test
    void parsePortMapNormalizesMacAndIgnoresGarbage() {
        Map<String, String> m = MeshGwnStatusScheduler.parsePortMap("24:0B:2B:47:2A:22=4, 240b2b46f83a =6, bad=, =7");
        assertEquals("4", m.get("240B2B472A22"));
        assertEquals("6", m.get("240B2B46F83A"));
        assertEquals(2, m.size());
    }
}
