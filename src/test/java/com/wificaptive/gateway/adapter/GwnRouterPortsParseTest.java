package com.wificaptive.gateway.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.gateway.dto.GwnRouterPorts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra parse portInfo của /oapi/v1.0.0/device/info bằng JSON THẬT lấy từ GWN7002 (dump 25/11). */
class GwnRouterPortsParseTest {

    private static final String PORT_INFO = """
        [
          {"portName":"WAN","silkScreenPort":"1","role":"1","type":"1","linkStatus":"0"},
          {"portName":"LAN2","silkScreenPort":"2","role":"0","type":"1","linkStatus":"0"},
          {"portName":"WAN","silkScreenPort":"3","role":"1","type":"0","linkStatus":"1"},
          {"portName":"LAN4","silkScreenPort":"4","role":"0","type":"0","linkStatus":"0"},
          {"portName":"LAN5","silkScreenPort":"5","role":"0","type":"0","linkStatus":"1"},
          {"portName":"LAN6","silkScreenPort":"6","role":"0","type":"0","linkStatus":"1"}
        ]
        """;

    @Test
    void parsesOnlyLanPortsWithCorrectLinkStatus() throws Exception {
        GwnRouterPorts ports = GwnCloudGatewayAdapter.parseRouterPorts(new ObjectMapper().readTree(PORT_INFO));

        assertTrue(ports.apiReachable());
        assertTrue(ports.hasLanInfo());
        // chỉ cổng LAN RJ45 (role=0, type=0): 4,5,6 — bỏ WAN (role=1) silk 1 & 3, và bỏ cổng SFP (type=1) silk 2
        assertNull(ports.isUp("1"));
        assertNull(ports.isUp("2")); // LAN2 là cổng SFP quang -> không tính là cổng AP
        assertNull(ports.isUp("3"));
        assertEquals(Boolean.FALSE, ports.isUp("4")); // mesh .111 rút khỏi LAN4
        assertEquals(Boolean.TRUE, ports.isUp("5"));  // mesh khác cắm LAN5
        assertEquals(Boolean.TRUE, ports.isUp("6"));  // mesh khác cắm LAN6
        assertTrue(ports.anyLanUp());                 // vì LAN5/6 up -> đây là lý do anyLanUp báo sai cho .111
    }
}
