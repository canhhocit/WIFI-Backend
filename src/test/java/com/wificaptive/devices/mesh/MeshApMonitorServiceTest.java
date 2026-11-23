package com.wificaptive.devices.mesh;

import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.incidents.service.IncidentTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MeshApMonitorServiceTest {

    private static final String HOST = "192.168.80.111";
    private static final String SERIAL = "VTGRBTEST0001";
    private static final String MANAGED = "Managed#2026";

    private MeshApClient client;
    private NetworkDeviceRepository repo;
    private IncidentTicketService incidents;
    private PlatformTransactionManager txm;
    private NetworkDevice device;

    @BeforeEach
    void setUp() {
        client = mock(MeshApClient.class);
        repo = mock(NetworkDeviceRepository.class);
        incidents = mock(IncidentTicketService.class);
        txm = mock(PlatformTransactionManager.class);
        when(txm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        device = new NetworkDevice();
        device.setId(7L);
        device.setDeviceType("ACCESS_POINT");
        device.setModel("vAP-32X6V1");
        device.setVendor("Viettel High Tech");
        device.setMacAddress("24:0B:2B:47:2A:22");
        device.setSerialNumber(SERIAL);
        device.setIpAddress(HOST);
        device.setStatus("UNKNOWN");
        when(repo.findById(7L)).thenReturn(Optional.of(device));
    }

    private MeshApMonitorService service(String managed) {
        return new MeshApMonitorService(client, repo, incidents, txm, true, "admin", managed, true);
    }

    private static MeshApSnapshotDto live() {
        Instant now = Instant.now();
        return new MeshApSnapshotDto(null, null, "LIVE", "ok", now, now, SERIAL, "vAP-32X6V1", "V1.6.6_VN",
                "vthomeap6", null, 100L, 30, 0.5, List.of(), List.of(), null);
    }

    private static MeshApException authFailed() {
        return new MeshApException(MeshApException.Kind.AUTH_FAILED, "denied");
    }

    @Test
    void firstContactUsesSerialThenRotatesToManagedPassword() throws Exception {
        when(client.fetchSnapshot(HOST, "admin", SERIAL)).thenReturn(live());
        when(client.fetchSnapshot(HOST, "admin", MANAGED)).thenReturn(live());

        MeshApSnapshotDto s = service(MANAGED).refresh(device);

        assertTrue(s.isLive());
        assertEquals(NetworkDevice.CRED_MANAGED, s.credentialState());
        verify(client).changePassword(HOST, "admin", MANAGED);
        assertEquals(NetworkDevice.CRED_MANAGED, device.getMeshCredentialState());
        assertNotNull(device.getMeshPasswordRotatedAt());
        assertEquals("ONLINE", device.getStatus());
    }

    @Test
    void managedDeviceTriesManagedPasswordFirstAndDoesNotRotateAgain() throws Exception {
        device.setMeshCredentialState(NetworkDevice.CRED_MANAGED);
        when(client.fetchSnapshot(HOST, "admin", MANAGED)).thenReturn(live());

        MeshApSnapshotDto s = service(MANAGED).refresh(device);

        assertTrue(s.isLive());
        verify(client, never()).fetchSnapshot(HOST, "admin", SERIAL);
        verify(client, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    void withoutManagedPasswordKeepsSerial() throws Exception {
        when(client.fetchSnapshot(HOST, "admin", SERIAL)).thenReturn(live());

        MeshApSnapshotDto s = service("").refresh(device);

        assertEquals(NetworkDevice.CRED_FACTORY, s.credentialState());
        verify(client, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    void deviceAlreadyChangedByHandIsDetectedAsManaged() throws Exception {
        when(client.fetchSnapshot(HOST, "admin", SERIAL)).thenThrow(authFailed());
        when(client.fetchSnapshot(HOST, "admin", MANAGED)).thenReturn(live());

        MeshApSnapshotDto s = service(MANAGED).refresh(device);

        assertEquals(NetworkDevice.CRED_MANAGED, s.credentialState());
        verify(client, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    void failedVerificationAfterRotationStaysFactoryAndSelfHealsNextPoll() throws Exception {
        when(client.fetchSnapshot(HOST, "admin", SERIAL)).thenReturn(live());
        when(client.fetchSnapshot(HOST, "admin", MANAGED)).thenThrow(authFailed());

        MeshApSnapshotDto s = service(MANAGED).refresh(device);

        assertTrue(s.isLive());
        assertEquals(NetworkDevice.CRED_FACTORY, s.credentialState());
        assertNull(device.getMeshPasswordRotatedAt());
    }

    @Test
    void bothPasswordsRejectedIsDegraded() throws Exception {
        when(client.fetchSnapshot(eq(HOST), eq("admin"), anyString())).thenThrow(authFailed());

        MeshApSnapshotDto s = service(MANAGED).refresh(device);

        assertEquals("AUTH_FAILED", s.monitoringMode());
        assertEquals("DEGRADED", device.getStatus());
    }

    @Test
    void unreachableGoesOfflineAndOpensIncident() throws Exception {
        device.setStatus("ONLINE");
        when(client.fetchSnapshot(eq(HOST), eq("admin"), anyString()))
                .thenThrow(new MeshApException(MeshApException.Kind.UNREACHABLE, "timeout"));

        MeshApSnapshotDto s = service(MANAGED).refresh(device);

        assertEquals("UNREACHABLE", s.monitoringMode());
        assertEquals("OFFLINE", device.getStatus());
        verify(incidents).recordDeviceFailureIncident(any(), eq(7L), anyString(), eq("24:0B:2B:47:2A:22"), anyString());
    }
}
