package com.wificaptive.devices.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.incidents.service.IncidentTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MeshAgentControllerTest {

    private static final String TOKEN = "agent-token-for-test";
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();
    private NetworkDeviceRepository repo;
    private MeshApMonitorService monitor;
    private MeshAgentController controller;
    private NetworkDevice device;

    @BeforeEach
    void setUp() throws Exception {
        repo = mock(NetworkDeviceRepository.class);
        PlatformTransactionManager txm = mock(PlatformTransactionManager.class);
        when(txm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        // Máy chủ KHÔNG bật đọc trực tiếp (giống SIT) — dữ liệu chỉ đến từ agent
        monitor = new MeshApMonitorService(mock(MeshApClient.class), repo, mock(IncidentTicketService.class), txm,
                false, "admin", "", true);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(TOKEN.getBytes(StandardCharsets.UTF_8)));
        controller = new MeshAgentController(monitor, repo, om, hash, true);

        device = new NetworkDevice();
        device.setId(5L);
        device.setDeviceType("ACCESS_POINT");
        device.setModel("vAP-32X6V1");
        device.setMacAddress("24:0B:2B:47:2A:22");
        device.setIpAddress("192.168.80.111");
        device.setStatus("UNKNOWN");
        when(repo.findByMacAddress("24:0B:2B:47:2A:22")).thenReturn(Optional.of(device));
        when(repo.findById(5L)).thenReturn(Optional.of(device));
        when(repo.findAll()).thenReturn(List.of(device));
    }

    private JsonNode body(String json) throws Exception {
        return om.readTree(json);
    }

    @Test
    void rejectsMissingOrWrongToken() throws Exception {
        assertEquals(401, controller.targets(null).getStatusCode().value());
        assertEquals(401, controller.snapshots("wrong", body("{\"devices\":[]}")).getStatusCode().value());
        MeshAgentController disabled = new MeshAgentController(monitor, repo, om, "", true);
        assertEquals(401, disabled.targets(TOKEN).getStatusCode().value());
    }

    @Test
    void listsTargets() {
        ResponseEntity<?> r = controller.targets(TOKEN);
        assertEquals(200, r.getStatusCode().value());
        assertTrue(r.getBody().toString().contains("192.168.80.111"));
    }

    @Test
    void liveSnapshotFromAgentMakesApOnlineEvenWhenDirectPollingDisabled() throws Exception {
        String topo = om.writeValueAsString("{\"topology information\":[{\"Serial number\":\"VTGRB0000TEST\","
                + "\"Firmware version\":\"V1.6.6_VN\",\"Product class\":\"vAP-32X6V1\",\"Radio Info\":[{\"channel\":108,"
                + "\"BSSINFO\":[{\"BSSID\":\"26:0b:2b:47:2a:23\",\"SSID\":\"VIETTEL_TEST\",\"Security\":\"0020\",\"Hidden\":0,"
                + "\"connected sta info\":[{\"STA MAC address\":\"a0:51:0b:30:8c:98\",\"BH STA\":\"No\",\"Medium\":\"5G\","
                + "\"uplink speed\":\"433\"}]}]}]}]}");
        String json = "{\"agentId\":\"lan-demo\",\"devices\":[{\"mac\":\"240b2b472a22\",\"host\":\"192.168.80.111\",\"mode\":\"LIVE\","
                + "\"topology\":" + topo + ",\"systemInfo\":{\"uptime\":100,\"memory\":{\"total\":1000,\"available\":250}},"
                + "\"signals\":{\"a0:51:0b:30:8c:98\":-41}},{\"mac\":\"11:22:33:44:55:66\"}]}";

        ResponseEntity<?> r = controller.snapshots(TOKEN, body(json));

        Map<?, ?> res = (Map<?, ?>) r.getBody();
        assertEquals(1, res.get("accepted"));
        assertEquals(List.of("11:22:33:44:55:66"), res.get("unknown"));
        assertEquals("ONLINE", device.getStatus());
        assertEquals("V1.6.6_VN", device.getFirmwareVersion());
        assertEquals(MeshApMonitorService.SOURCE_MESH_LIVE, monitor.monitoringSource(device));
        MeshApSnapshotDto s = monitor.getSnapshot(device, true);
        assertTrue(s.isLive());
        assertEquals(1, s.clients().size());
        assertEquals(-41, s.clients().get(0).rssi());
    }

    @Test
    void acceptsRawPayloadFromScriptRunningOnMeshAndMatchesByHostWhenMacDiffers() throws Exception {
        // Script trên mesh gửi nguyên getTopologyDump + iwinfo.assoclist theo interface; MAC br-lan có thể khác MAC khai báo
        String topoDump = om.writeValueAsString(om.createObjectNode().put("status", "SUCCESS").put("luaTopologyInfo",
                "{\"topology information\":[{\"Serial number\":\"VTGRB0000TEST\",\"Firmware version\":\"V1.6.6_VN\","
                        + "\"Product class\":\"vAP-32X6V1\",\"Radio Info\":[{\"channel\":108,\"BSSINFO\":[{\"BSSID\":\"26:0b:2b:47:2a:23\","
                        + "\"SSID\":\"VIETTEL_TEST\",\"Security\":\"0001\",\"Hidden\":0,\"connected sta info\":[{\"STA MAC address\":"
                        + "\"a0:51:0b:30:8c:98\",\"BH STA\":\"No\",\"Medium\":\"5G\",\"uplink speed\":\"433\"}]}]}]}]}"));
        String json = "{\"agentId\":\"mesh-24:0b:2b:47:2a:21\",\"devices\":[{\"mac\":\"24:0b:2b:47:2a:21\",\"host\":\"192.168.80.111\","
                + "\"mode\":\"LIVE\",\"topology\":" + topoDump + ",\"systemInfo\":{\"uptime\":5},"
                + "\"assoc\":{\"ra0\":{\"results\":[]},\"rax0\":{\"results\":[{\"mac\":\"A0:51:0B:30:8C:98\",\"signal\":-54}]}}}]}";

        Map<?, ?> res = (Map<?, ?>) controller.snapshots(TOKEN, body(json)).getBody();

        assertEquals(1, res.get("accepted"));
        assertEquals("ONLINE", device.getStatus());
        MeshApSnapshotDto s = monitor.getSnapshot(device, false);
        assertEquals(-54, s.clients().get(0).rssi());
        assertTrue(s.message().contains("mesh-24:0b:2b:47:2a:21"));
    }

    @Test
    void agentReportedUnreachableGoesOffline() throws Exception {
        device.setStatus("ONLINE");
        controller.snapshots(TOKEN, body("{\"devices\":[{\"mac\":\"24:0B:2B:47:2A:22\",\"mode\":\"UNREACHABLE\",\"message\":\"timeout\"}]}"));
        assertEquals("OFFLINE", device.getStatus());
        assertEquals("MESH_UNREACHABLE", monitor.monitoringSource(device));
    }

    @Test
    void staleAgentDataExpiresToOffline() throws Exception {
        MeshApMonitorService fast = new MeshApMonitorService(mock(MeshApClient.class), repo, mock(IncidentTicketService.class),
                mockTx(), false, "admin", "", true, 0L);
        MeshAgentController c = new MeshAgentController(fast, repo, om,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(TOKEN.getBytes(StandardCharsets.UTF_8))), true);
        c.snapshots(TOKEN, body("{\"devices\":[{\"mac\":\"24:0B:2B:47:2A:22\",\"mode\":\"LIVE\",\"topology\":\"{}\"}]}"));
        assertEquals("ONLINE", device.getStatus());
        Thread.sleep(5);
        fast.expireAgentSnapshots();
        assertEquals("OFFLINE", device.getStatus());
    }

    @Test
    void autoProvisionsUnknownMeshFromLiveSelfPush() throws Exception {
        // Mesh mới chưa có trong CSDL -> server tự tạo thiết bị từ dữ liệu mesh tự báo (không seed cứng).
        when(repo.findByMacAddress("24:0B:2B:99:88:77")).thenReturn(Optional.empty());
        NetworkDevice created = new NetworkDevice();
        created.setId(77L);
        when(repo.save(any(NetworkDevice.class))).thenAnswer(inv -> {
            NetworkDevice d = inv.getArgument(0);
            created.setDeviceType(d.getDeviceType());
            created.setMacAddress(d.getMacAddress());
            created.setSerialNumber(d.getSerialNumber());
            created.setModel(d.getModel());
            created.setVendor(d.getVendor());
            created.setIpAddress(d.getIpAddress());
            return created;
        });
        when(repo.findById(77L)).thenReturn(Optional.of(created));

        String topo = om.writeValueAsString("{\"topology information\":[{\"Serial number\":\"VTNEWAP0001\","
                + "\"Firmware version\":\"V1.6.6_VN\",\"Product class\":\"vAP-32X6V1\",\"Radio Info\":[{\"channel\":36,"
                + "\"BSSINFO\":[{\"BSSID\":\"26:0b:2b:99:88:78\",\"SSID\":\"VIETTEL_NEW\",\"Security\":\"0020\",\"Hidden\":0,"
                + "\"connected sta info\":[]}]}]}]}");
        String json = "{\"agentId\":\"mesh-24:0b:2b:99:88:77\",\"devices\":[{\"mac\":\"24:0B:2B:99:88:77\","
                + "\"host\":\"192.168.80.150\",\"mode\":\"LIVE\",\"topology\":" + topo + ",\"systemInfo\":{\"uptime\":42}}]}";

        Map<?, ?> res = (Map<?, ?>) controller.snapshots(TOKEN, body(json)).getBody();

        assertEquals(1, res.get("accepted"));
        assertEquals(List.of(), res.get("unknown"));
        ArgumentCaptor<NetworkDevice> cap = ArgumentCaptor.forClass(NetworkDevice.class);
        verify(repo, atLeastOnce()).save(cap.capture());
        NetworkDevice provisioned = cap.getAllValues().get(0);
        assertEquals("ACCESS_POINT", provisioned.getDeviceType());
        assertEquals("24:0B:2B:99:88:77", provisioned.getMacAddress());
        assertEquals("VTNEWAP0001", provisioned.getSerialNumber());
        assertEquals("vAP-32X6V1", provisioned.getModel());
        assertEquals("192.168.80.150", provisioned.getIpAddress());
        assertEquals("ONLINE", created.getStatus()); // ingest -> applyToDevice đưa về ONLINE
        assertEquals(MeshApMonitorService.SOURCE_MESH_LIVE, monitor.monitoringSource(created));
    }

    @Test
    void doesNotAutoProvisionWhenDisabled() throws Exception {
        MeshAgentController noProv = new MeshAgentController(monitor, repo, om,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(TOKEN.getBytes(StandardCharsets.UTF_8))), false);
        when(repo.findByMacAddress("24:0B:2B:99:88:77")).thenReturn(Optional.empty());
        String json = "{\"devices\":[{\"mac\":\"24:0B:2B:99:88:77\",\"host\":\"192.168.80.150\",\"mode\":\"LIVE\",\"topology\":\"{}\"}]}";

        Map<?, ?> res = (Map<?, ?>) noProv.snapshots(TOKEN, body(json)).getBody();

        assertEquals(0, res.get("accepted"));
        assertEquals(List.of("24:0B:2B:99:88:77"), res.get("unknown"));
        verify(repo, never()).save(any());
    }

    @Test
    void autoBindsLocationWhenAgentIdMatchesLocationCode() throws Exception {
        com.wificaptive.locations.repository.WifiLocationRepository locRepo =
                mock(com.wificaptive.locations.repository.WifiLocationRepository.class);
        controller.setLocationRepository(locRepo);

        com.wificaptive.locations.entity.WifiLocation loc = new com.wificaptive.locations.entity.WifiLocation();
        loc.setId(12L);
        loc.setName("Nhà văn hóa Số 1");
        loc.setLocationCode("NVH-0001");

        when(locRepo.findByLocationCodeAndDeletedFalse("NVH-0001")).thenReturn(Optional.of(loc));
        when(repo.findByMacAddress("24:0B:2B:99:88:66")).thenReturn(Optional.empty());
        when(repo.findBySerialNumber("VTNEWAP0002")).thenReturn(Optional.empty());
        NetworkDevice created = new NetworkDevice();
        created.setId(88L);
        created.setDeviceType("ACCESS_POINT");
        created.setMacAddress("24:0B:2B:99:88:66");
        created.setSerialNumber("VTNEWAP0002");
        created.setModel("vAP-32X6V1");
        created.setVendor("Viettel High Tech");
        created.setLocation(loc);
        when(repo.save(any(NetworkDevice.class))).thenReturn(created);
        when(repo.findById(88L)).thenReturn(Optional.of(created));

        String topo = om.writeValueAsString("{\"topology information\":[{\"Serial number\":\"VTNEWAP0002\","
                + "\"Firmware version\":\"V1.6.6_VN\",\"Product class\":\"vAP-32X6V1\",\"Radio Info\":[]}]}");
        String json = "{\"agentId\":\"NVH-0001\",\"devices\":[{\"mac\":\"24:0B:2B:99:88:66\","
                + "\"host\":\"192.168.80.151\",\"mode\":\"LIVE\",\"topology\":" + topo + ",\"systemInfo\":{\"uptime\":10}}]}";

        Map<?, ?> res = (Map<?, ?>) controller.snapshots(TOKEN, body(json)).getBody();

        assertEquals(1, res.get("accepted"));
        ArgumentCaptor<NetworkDevice> cap = ArgumentCaptor.forClass(NetworkDevice.class);
        verify(repo, atLeastOnce()).save(cap.capture());
        assertEquals(12L, cap.getValue().getLocation().getId());
    }

    private static PlatformTransactionManager mockTx() {
        PlatformTransactionManager txm = mock(PlatformTransactionManager.class);
        when(txm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return txm;
    }
}
