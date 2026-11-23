package com.wificaptive.devices.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dữ liệu mẫu lấy từ mesh vAP-32X6V1 firmware V1.6.6_VN thật tại điểm demo (đã bỏ mật khẩu Wi-Fi).
 */
class MeshApParserTest {

    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

    private static final String TOPOLOGY = """
        {"topology information":[{"AL MAC":"00:00:00:00:00:ff","Serial number":"VTGRB0000TEST",
          "Firmware version":"V1.6.6_VN","Product class":"vAP-32X6V1","Device role":0,
          "Number of radios":2,
          "Radio Info":[
            {"channel":108,"BSSINFO":[{"BSSID":"26:0b:2b:47:2a:23","SSID":"VIETTEL_TEST","Security":"0001",
              "Encryption":"0001","Pass-phrase":"secret","Hidden":0,
              "connected sta info":[{"STA MAC address":"a0:51:0b:30:8c:98","Tp Tx Dl KBps":0,"Tp Rx Ul KBps":0,
                "BH STA":"No","Medium":"5G","uplink speed":"433","uplink rssi":"NA"},
                {"STA MAC address":"aa:bb:cc:dd:ee:ff","BH STA":"Yes","Medium":"5G"}]}]},
            {"channel":3,"BSSINFO":[{"BSSID":"24:0b:2b:47:2a:23","SSID":"VIETTEL_TEST","Security":"0020",
              "Pass-phrase":"secret","Hidden":0}]}
          ]}]}
        """;

    private static final String SYSTEM_INFO = """
        {"uptime":48462,"load":[65536,65952,65536],
         "memory":{"total":498704384,"free":295702528,"available":332599296}}
        """;

    private static final String BOARD = """
        {"hostname":"vthomeap6","model":"MediaTek MT7981",
         "release":{"description":"OpenWrt 21.02-SNAPSHOT r0-449b1c931"}}
        """;

    private static final String HOST_HINTS = """
        {"A0:51:0B:30:8C:98":{"ipaddrs":["192.168.80.137"],"ip6addrs":[],"name":"laptop"}}
        """;

    @Test
    void parsesRealDeviceTopology() throws Exception {
        MeshApSnapshotDto s = MeshApParser.parse(om.readTree(TOPOLOGY), om.readTree(SYSTEM_INFO),
                om.readTree(BOARD), om.readTree(HOST_HINTS), Map.of("A0:51:0B:30:8C:98", -38));

        assertTrue(s.isLive());
        assertEquals("VTGRB0000TEST", s.serialNumber());
        assertEquals("vAP-32X6V1", s.model());
        assertEquals("V1.6.6_VN", s.firmwareVersion());
        assertEquals("vthomeap6", s.hostname());
        assertEquals(48462L, s.uptimeSeconds());
        assertEquals(33, s.memoryUsagePercent());
        assertEquals(1.0, s.loadAverage1m());

        assertEquals(2, s.radios().size());
        assertEquals("5GHz", s.radios().get(0).band());
        assertEquals(108, s.radios().get(0).channel());
        assertEquals("OPEN", s.radios().get(0).ssids().get(0).security());
        assertEquals("2.4GHz", s.radios().get(1).band());
        assertEquals("WPA2", s.radios().get(1).ssids().get(0).security());

        // backhaul STA bị loại, chỉ còn người dùng thật
        assertEquals(1, s.clients().size());
        MeshApSnapshotDto.Client c = s.clients().get(0);
        assertEquals("A0:51:0B:30:8C:98", c.mac());
        assertEquals("192.168.80.137", c.ip());
        assertEquals("laptop", c.hostname());
        assertEquals("5GHz", c.band());
        assertEquals("VIETTEL_TEST", c.ssid());
        assertEquals(433, c.linkSpeedMbps());
        assertEquals(-38, c.rssi()); // "NA" trong topology -> lấy từ iwinfo.assoclist
    }

    @Test
    void neverExposesPassphrase() throws Exception {
        MeshApSnapshotDto s = MeshApParser.parse(om.readTree(TOPOLOGY), null, null, null, Map.of());
        String json = om.writeValueAsString(s);
        assertFalse(json.contains("secret"));
        assertNull(s.clients().get(0).rssi());
    }

    @Test
    void toleratesEmptyTopology() throws Exception {
        JsonNode empty = om.readTree("{}");
        MeshApSnapshotDto s = MeshApParser.parse(empty, null, null, null, Map.of());
        assertTrue(s.isLive());
        assertEquals(List.of(), s.clients());
        assertEquals(List.of(), s.radios());
    }

    @Test
    void extractsSysauthCookie() {
        assertEquals("abc123", MeshApClient.extractSysauth(
                List.of("foo=bar; path=/", "sysauth_https=abc123; path=/cgi-bin/luci/; SameSite=Strict; Secure; HttpOnly")));
        assertNull(MeshApClient.extractSysauth(List.of("sysauth=deleted; path=/")));
        assertNull(MeshApClient.extractSysauth(null));
    }

    @Test
    void rejectsPublicHosts() {
        assertThrows(MeshApException.class, () -> MeshApClient.ensurePrivateHost("8.8.8.8"));
        assertThrows(MeshApException.class, () -> MeshApClient.ensurePrivateHost("evil.example.com"));
        assertDoesNotThrow(() -> MeshApClient.ensurePrivateHost("192.168.80.111"));
        assertDoesNotThrow(() -> MeshApClient.ensurePrivateHost("10.8.0.5"));
    }
}
