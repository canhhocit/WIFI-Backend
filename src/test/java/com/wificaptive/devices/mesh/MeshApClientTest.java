package com.wificaptive.devices.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm tra luồng HTTP thật của MeshApClient với một "mesh giả lập": HTTPS chứng chỉ tự ký,
 * đăng nhập form LuCI trả cookie sysauth_https, JSON-RPC /ubus giống firmware V1.6.6_VN.
 */
class MeshApClientTest {

    private static final String USER = "admin";
    private static final String FACTORY_PASS = "TEST-SERIAL-001";
    private volatile String PASS = FACTORY_PASS;

    private final ObjectMapper om = new ObjectMapper();
    private HttpsServer server;
    private String host;
    private final AtomicInteger logins = new AtomicInteger();
    private volatile String validSid = "sid-1";

    @BeforeEach
    void start() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = getClass().getResourceAsStream("/mesh/mesh-test.p12")) {
            ks.load(in, "meshtest".toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "meshtest".toCharArray());
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), null, null);

        server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(ssl));
        server.createContext("/cgi-bin/luci/", this::handleLogin);
        server.createContext("/ubus", this::handleUbus);
        server.start();
        host = "127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void logsInLikeBrowserAndReadsSnapshot() throws Exception {
        MeshApClient client = new MeshApClient(om, 3000);
        MeshApSnapshotDto s = client.fetchSnapshot(host, USER, PASS);

        assertTrue(s.isLive());
        assertEquals("VTGRB0000TEST", s.serialNumber());
        assertEquals("V1.6.6_VN", s.firmwareVersion());
        assertEquals(1, s.clients().size());
        assertEquals("A0:51:0B:30:8C:98", s.clients().get(0).mac());
        assertEquals("192.168.80.137", s.clients().get(0).ip());
        assertEquals(-41, s.clients().get(0).rssi());   // từ iwinfo.assoclist
        assertEquals(433, s.clients().get(0).linkSpeedMbps());
        assertEquals(1, logins.get());

        // Lần 2 dùng lại phiên, không đăng nhập lại
        client.fetchSnapshot(host, USER, PASS);
        assertEquals(1, logins.get());
    }

    @Test
    void reloginsWhenSessionExpires() throws Exception {
        MeshApClient client = new MeshApClient(om, 3000);
        client.fetchSnapshot(host, USER, PASS);
        validSid = "sid-rotated"; // thiết bị khởi động lại -> phiên cũ hết hạn
        MeshApSnapshotDto s = client.fetchSnapshot(host, USER, PASS);
        assertTrue(s.isLive());
        assertEquals(2, logins.get());
    }

    @Test
    void changesPasswordAndOldOneStopsWorking() throws Exception {
        MeshApClient client = new MeshApClient(om, 3000);
        client.fetchSnapshot(host, USER, FACTORY_PASS);
        client.changePassword(host, USER, "Managed#2026");
        assertEquals("Managed#2026", PASS);
        assertTrue(client.fetchSnapshot(host, USER, "Managed#2026").isLive());
        client.forgetSession(host);
        MeshApException e = assertThrows(MeshApException.class, () -> client.fetchSnapshot(host, USER, FACTORY_PASS));
        assertEquals(MeshApException.Kind.AUTH_FAILED, e.getKind());
    }

    @Test
    void wrongPasswordIsAuthFailed() {
        MeshApClient client = new MeshApClient(om, 3000);
        MeshApException e = assertThrows(MeshApException.class, () -> client.fetchSnapshot(host, USER, "wrong"));
        assertEquals(MeshApException.Kind.AUTH_FAILED, e.getKind());
    }

    @Test
    void closedPortIsUnreachable() throws Exception {
        int port = server.getAddress().getPort();
        server.stop(0);
        MeshApClient client = new MeshApClient(om, 1500);
        MeshApException e = assertThrows(MeshApException.class,
                () -> client.fetchSnapshot("127.0.0.1:" + port, USER, PASS));
        assertEquals(MeshApException.Kind.UNREACHABLE, e.getKind());
        server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0); // để @AfterEach stop được
    }

    // ------------------------------------------------------------------------------------------

    private void handleLogin(HttpExchange ex) throws IOException {
        Map<String, String> form = new HashMap<>();
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        for (String kv : body.split("&")) {
            String[] p = kv.split("=", 2);
            if (p.length == 2) form.put(p[0], URLDecoder.decode(p[1], StandardCharsets.UTF_8));
        }
        if ("POST".equals(ex.getRequestMethod()) && USER.equals(form.get("luci_username"))
                && PASS.equals(form.get("luci_password"))) {
            logins.incrementAndGet();
            ex.getResponseHeaders().add("Set-Cookie", "sysauth_https=" + validSid + "; path=/cgi-bin/luci/; Secure; HttpOnly");
            ex.getResponseHeaders().add("Location", "/cgi-bin/luci/");
            ex.sendResponseHeaders(302, -1);
        } else {
            byte[] page = "<html>login</html>".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(403, page.length);
            ex.getResponseBody().write(page);
        }
        ex.close();
    }

    private void handleUbus(HttpExchange ex) throws IOException {
        JsonNode req = om.readTree(ex.getRequestBody());
        JsonNode params = req.path("params");
        String sid = params.path(0).asText();
        String obj = params.path(1).asText();
        String method = params.path(2).asText();

        ObjectNode resp = om.createObjectNode();
        resp.put("jsonrpc", "2.0");
        resp.set("id", req.path("id"));
        if (!validSid.equals(sid)) {
            ObjectNode err = resp.putObject("error");
            err.put("code", -32002);
            err.put("message", "Access denied");
        } else {
            String data = switch (obj + "." + method) {
                case "dat2wifi.getTopologyDump" -> om.createObjectNode().put("status", "SUCCESS").put("luaTopologyInfo",
                        "{\"topology information\":[{\"Serial number\":\"VTGRB0000TEST\",\"Firmware version\":\"V1.6.6_VN\","
                                + "\"Product class\":\"vAP-32X6V1\",\"Radio Info\":[{\"channel\":108,\"BSSINFO\":[{\"BSSID\":\"26:0b:2b:47:2a:23\","
                                + "\"SSID\":\"VIETTEL_TEST\",\"Security\":\"0001\",\"Pass-phrase\":\"x\",\"Hidden\":0,\"connected sta info\":"
                                + "[{\"STA MAC address\":\"a0:51:0b:30:8c:98\",\"BH STA\":\"No\",\"Medium\":\"5G\",\"uplink speed\":\"433\","
                                + "\"uplink rssi\":\"NA\"}]}]}]}]}").toString();
                case "system.info" -> "{\"uptime\":100,\"load\":[32768,0,0],\"memory\":{\"total\":1000,\"available\":250}}";
                case "system.board" -> "{\"hostname\":\"vthomeap6\",\"release\":{\"description\":\"OpenWrt 21.02\"}}";
                case "luci-rpc.getHostHints" -> "{\"A0:51:0B:30:8C:98\":{\"ipaddrs\":[\"192.168.80.137\"]}}";
                case "dat2wifi.getWifiInfo" -> "{\"1\":{\"vifs\":{\"1\":{\"vifname\":\"rax0\",\"state\":\"up\"},\"2\":{\"vifname\":\"rax1\",\"state\":\"down\"}}}}";
                case "luci.setPassword" -> {
                    PASS = params.path(3).path("password").asText();
                    yield "{\"result\":true}";
                }
                case "iwinfo.assoclist" -> "{\"results\":[{\"mac\":\"A0:51:0B:30:8C:98\",\"signal\":-41}]}";
                default -> null;
            };
            if (data == null) {
                ObjectNode err = resp.putObject("error");
                err.put("code", -32002);
                err.put("message", "Access denied");
            } else {
                resp.putArray("result").add(0).add(om.readTree(data));
            }
        }
        byte[] out = om.writeValueAsBytes(resp);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }
}
