package com.wificaptive.devices.mesh;

import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.gateway.adapter.GwnCloudGatewayAdapter;
import com.wificaptive.gateway.dto.GwnRouterPorts;
import com.wificaptive.incidents.dto.CreateIncidentRequest;
import com.wificaptive.incidents.service.IncidentTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GwnPortIncidentSchedulerTest {

    private GwnCloudGatewayAdapter gwn;
    private NetworkDeviceRepository repo;
    private IncidentTicketService incidents;
    private GwnPortIncidentScheduler scheduler;

    @BeforeEach
    void setUp() {
        gwn = mock(GwnCloudGatewayAdapter.class);
        repo = mock(NetworkDeviceRepository.class);
        incidents = mock(IncidentTicketService.class);

        NetworkDevice router = new NetworkDevice();
        router.setId(1L);
        router.setDeviceType("ROUTER");
        router.setModel("GWN7002");

        NetworkDevice meshOnLan4 = new NetworkDevice();
        meshOnLan4.setId(2L);
        meshOnLan4.setDeviceType("ACCESS_POINT");
        meshOnLan4.setModel("vAP-32X6V1");
        meshOnLan4.setVendor("Viettel High Tech");
        meshOnLan4.setMacAddress("24:0B:2B:47:2A:22");
        meshOnLan4.setGwnLanPort("4");

        when(repo.findRouters()).thenReturn(List.of(router));
        when(repo.findAccessPoints()).thenReturn(List.of(meshOnLan4));
        scheduler = new GwnPortIncidentScheduler(gwn, repo, incidents);
    }

    private static GwnRouterPorts ports(boolean lan5Up) {
        Map<String, Boolean> m = new LinkedHashMap<>();
        m.put("4", false); // cổng đã gán mesh (bound) -> không mở ở đây
        m.put("5", lan5Up);
        return new GwnRouterPorts(true, m);
    }

    @Test
    void opensIncidentOnlyWhenUnboundPortTransitionsOffline() {
        when(gwn.fetchRouterPorts()).thenReturn(ports(true), ports(false), ports(false));

        scheduler.checkGwnPortIncidents(); // baseline: LAN5 up -> chưa có gì
        verify(incidents, never()).createTicket(any(), any());

        scheduler.checkGwnPortIncidents(); // LAN5 up->down -> mở 1 phiếu
        ArgumentCaptor<CreateIncidentRequest> cap = ArgumentCaptor.forClass(CreateIncidentRequest.class);
        verify(incidents, times(1)).createTicket(cap.capture(), any());
        assertTrue(cap.getValue().getTitle().contains("LAN5"));

        scheduler.checkGwnPortIncidents(); // vẫn down -> không mở thêm (chống trùng)
        verify(incidents, times(1)).createTicket(any(), any());
    }

    @Test
    void autoResolvesWhenUnboundPortComesBackUp() {
        when(gwn.fetchRouterPorts()).thenReturn(ports(false), ports(true));

        scheduler.checkGwnPortIncidents(); // baseline: LAN5 down
        verify(incidents, never()).resolvePortRecoveredIncidents(any(), any(), any(), any());

        scheduler.checkGwnPortIncidents(); // LAN5 down->up -> tự đóng phiếu cổng LAN5
        verify(incidents, times(1)).resolvePortRecoveredIncidents(any(), any(), eq("5"), any());
    }
}
