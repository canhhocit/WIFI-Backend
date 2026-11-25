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
        when(repo.findAccessPoints()).thenReturn(List.of(mesh));
        when(repo.findRouters()).thenReturn(List.of());

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
    void usesPortFromDeviceRecordWhenSet() {
        // Cổng khai trên thiết bị (import) được ưu tiên, không cần env map
        mesh.setGwnLanPort("4");
        MeshGwnStatusScheduler s = new MeshGwnStatusScheduler(gwn, repo, monitor, "");
        s.refreshNow();
        verify(monitor).ingestGwnStatus(eq(mesh), eq(false), any()); // LAN4 down -> OFFLINE
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

    @Test
    void skipsMeshAtLocationWithoutRouterInsteadOfUsingDefaultEnvRouter() {
        com.wificaptive.locations.entity.WifiLocation loc2 = new com.wificaptive.locations.entity.WifiLocation();
        loc2.setId(2L);
        mesh.setLocation(loc2);
        when(repo.findAccessPoints()).thenReturn(List.of(mesh));
        when(repo.findRouters()).thenReturn(List.of());

        MeshGwnStatusScheduler s = new MeshGwnStatusScheduler(gwn, repo, monitor, "");
        int updated = s.refreshNow();

        assertEquals(0, updated);
        verify(gwn, never()).fetchRouterPorts();
        verify(gwn, never()).fetchRouterPorts(any(), any());
        verify(monitor, never()).ingestGwnStatus(any(), anyBoolean(), any());
    }

    @Test
    void usesSiteRouterPortsForLocationWithAssignedRouter() {
        com.wificaptive.locations.entity.WifiLocation loc2 = new com.wificaptive.locations.entity.WifiLocation();
        loc2.setId(2L);
        mesh.setLocation(loc2);
        mesh.setGwnLanPort("5");

        NetworkDevice siteRouter = new NetworkDevice();
        siteRouter.setId(99L);
        siteRouter.setDeviceType("ROUTER");
        siteRouter.setMacAddress("AA:BB:CC:DD:EE:FF");
        siteRouter.setGwnNetworkId("3300");
        siteRouter.setLocation(loc2);

        Map<String, Boolean> lan = new LinkedHashMap<>();
        lan.put("5", true);
        when(repo.findAccessPoints()).thenReturn(List.of(mesh));
        when(repo.findRouters()).thenReturn(List.of(siteRouter));
        when(gwn.fetchRouterPorts("AABBCCDDEEFF", "3300")).thenReturn(new GwnRouterPorts(true, lan));

        MeshGwnStatusScheduler s = new MeshGwnStatusScheduler(gwn, repo, monitor, "");
        int updated = s.refreshNow();

        assertEquals(1, updated);
        verify(gwn).fetchRouterPorts("AABBCCDDEEFF", "3300");
        verify(gwn, never()).fetchRouterPorts();
        verify(monitor).ingestGwnStatus(eq(mesh), eq(true), any());
    }
}
