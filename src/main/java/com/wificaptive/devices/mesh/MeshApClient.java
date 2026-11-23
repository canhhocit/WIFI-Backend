package com.wificaptive.devices.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client đọc trực tiếp thiết bị mesh Viettel HomeWiFi vAP-32X6V1 (OpenWrt 21.02 + LuCI).
 *
 * Cách đăng nhập giống trình duyệt: POST form LuCI (luci_username / luci_password) -> cookie
 * {@code sysauth*} chính là ubus session id, sau đó gọi JSON-RPC tại {@code https://<ip>/ubus}.
 * Chỉ dùng các lệnh ĐỌC: dat2wifi.getTopologyDump, dat2wifi.getWifiInfo, system.info, system.board,
 * luci-rpc.getHostHints, iwinfo.assoclist.
 *
 * Thiết bị dùng chứng chỉ HTTPS tự ký nên bỏ kiểm tra chứng chỉ — chỉ cho phép với địa chỉ nội bộ
 * (RFC1918 / loopback / link-local), không bao giờ áp dụng cho địa chỉ Internet.
 */
@Component
public class MeshApClient {

    private static final Logger log = LoggerFactory.getLogger(MeshApClient.class);
    private static final String ANON_SESSION = "00000000000000000000000000000000";

    private final ObjectMapper objectMapper;
    private final int timeoutMs;
    private final SSLSocketFactory lanSocketFactory;
    private final Map<String, String> sessionByHost = new ConcurrentHashMap<>();
    private final AtomicLong rpcId = new AtomicLong();

    public MeshApClient(ObjectMapper objectMapper,
                        @Value("${app.mesh-ap.timeout-ms:5000}") int timeoutMs) {
        this.objectMapper = objectMapper;
        this.timeoutMs = timeoutMs;
        this.lanSocketFactory = buildLanSocketFactory();
    }

    /**
     * Đọc toàn bộ thông tin cần giám sát từ một thiết bị mesh.
     */
    public MeshApSnapshotDto fetchSnapshot(String host, String username, String password) throws MeshApException {
        ensurePrivateHost(host);
        String sid = sessionByHost.get(host);
        if (sid == null) {
            sid = login(host, username, password);
        }
        try {
            return collect(host, sid);
        } catch (MeshApException e) {
            if (e.getKind() != MeshApException.Kind.ACCESS_DENIED) throw e;
            // Phiên cũ hết hạn -> đăng nhập lại một lần
            sessionByHost.remove(host);
            sid = login(host, username, password);
            return collect(host, sid);
        }
    }

    public void forgetSession(String host) {
        sessionByHost.remove(host);
    }

    /**
     * Đổi mật khẩu quản trị của mesh (luci.setPassword) bằng phiên đang đăng nhập.
     * Gọi ngay sau {@link #fetchSnapshot} thành công với mật khẩu hiện tại.
     */
    public void changePassword(String host, String username, String newPassword) throws MeshApException {
        ensurePrivateHost(host);
        String sid = sessionByHost.get(host);
        if (sid == null) {
            throw new MeshApException(MeshApException.Kind.ACCESS_DENIED, "Chưa có phiên đăng nhập để đổi mật khẩu");
        }
        JsonNode res = callRequired(host, sid, "luci", "setPassword",
                Map.of("username", username, "password", newPassword));
        if (res.has("result") && !res.path("result").asBoolean(false)) {
            throw new MeshApException(MeshApException.Kind.PROTOCOL, "Thiết bị không chấp nhận mật khẩu mới");
        }
        sessionByHost.remove(host);
    }

    // ------------------------------------------------------------------------------------------

    private MeshApSnapshotDto collect(String host, String sid) throws MeshApException {
        JsonNode topoResult = callRequired(host, sid, "dat2wifi", "getTopologyDump", Map.of());
        JsonNode topology;
        try {
            String raw = topoResult.path("luaTopologyInfo").asText("");
            topology = raw.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(raw);
        } catch (IOException e) {
            throw new MeshApException(MeshApException.Kind.PROTOCOL, "Không đọc được dữ liệu topology của mesh", e);
        }

        JsonNode systemInfo = callOptional(host, sid, "system", "info", Map.of());
        JsonNode board = callOptional(host, sid, "system", "board", Map.of());
        JsonNode hostHints = callOptional(host, sid, "luci-rpc", "getHostHints", Map.of());
        Map<String, Integer> signals = readSignals(host, sid);

        return MeshApParser.parse(topology, systemInfo, board, hostHints, signals);
    }

    /** RSSI theo MAC từ iwinfo.assoclist trên các interface Wi-Fi đang bật (best-effort). */
    private Map<String, Integer> readSignals(String host, String sid) {
        Map<String, Integer> out = new HashMap<>();
        JsonNode wifiInfo = callOptional(host, sid, "dat2wifi", "getWifiInfo", Map.of());
        if (wifiInfo == null) return out;
        wifiInfo.fields().forEachRemaining(radioEntry -> {
            JsonNode vifs = radioEntry.getValue().path("vifs");
            vifs.fields().forEachRemaining(vifEntry -> {
                JsonNode vif = vifEntry.getValue();
                String ifname = vif.path("vifname").asText("");
                if (ifname.isBlank() || !"up".equalsIgnoreCase(vif.path("state").asText(""))) return;
                JsonNode assoc = callOptional(host, sid, "iwinfo", "assoclist", Map.of("device", ifname));
                if (assoc == null) return;
                for (JsonNode sta : assoc.path("results")) {
                    String mac = MeshApParser.upperMac(sta.path("mac").asText(null));
                    if (mac != null && sta.has("signal")) out.put(mac, sta.path("signal").asInt());
                }
            });
        });
        return out;
    }

    private String login(String host, String username, String password) throws MeshApException {
        String form = "luci_username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&luci_password=" + URLEncoder.encode(password == null ? "" : password, StandardCharsets.UTF_8);
        HttpsURLConnection conn = null;
        try {
            conn = open(host, "/cgi-bin/luci/");
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Referer", "https://" + host + "/cgi-bin/luci/");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(form.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            List<String> setCookies = new java.util.ArrayList<>();
            conn.getHeaderFields().forEach((name, values) -> {
                if (name != null && name.equalsIgnoreCase("Set-Cookie") && values != null) setCookies.addAll(values);
            });
            String sid = extractSysauth(setCookies);
            drain(conn);
            if (sid == null) {
                if (code == 403 || code == 200) {
                    throw new MeshApException(MeshApException.Kind.AUTH_FAILED,
                            "Thiết bị mesh từ chối đăng nhập (sai tài khoản hoặc mật khẩu)");
                }
                throw new MeshApException(MeshApException.Kind.PROTOCOL,
                        "Thiết bị mesh không trả phiên đăng nhập (HTTP " + code + ")");
            }
            sessionByHost.put(host, sid);
            return sid;
        } catch (IOException e) {
            throw new MeshApException(MeshApException.Kind.UNREACHABLE,
                    "Không kết nối được tới mesh " + host + ": " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    static String extractSysauth(List<String> setCookies) {
        if (setCookies == null) return null;
        for (String c : setCookies) {
            if (c == null) continue;
            String first = c.split(";", 2)[0].trim();
            int eq = first.indexOf('=');
            if (eq <= 0) continue;
            String name = first.substring(0, eq);
            String value = first.substring(eq + 1);
            if (name.startsWith("sysauth") && !value.isBlank() && !"deleted".equals(value)) {
                return value;
            }
        }
        return null;
    }

    private JsonNode callRequired(String host, String sid, String obj, String method, Map<String, Object> args)
            throws MeshApException {
        JsonNode resp = rpc(host, sid, obj, method, args);
        JsonNode err = resp.path("error");
        if (!err.isMissingNode()) {
            int code = err.path("code").asInt();
            if (code == -32002) {
                throw new MeshApException(MeshApException.Kind.ACCESS_DENIED, "Phiên ubus hết hạn hoặc không đủ quyền");
            }
            throw new MeshApException(MeshApException.Kind.PROTOCOL,
                    obj + "." + method + " lỗi " + code + ": " + err.path("message").asText(""));
        }
        JsonNode result = resp.path("result");
        if (!result.isArray() || result.size() == 0 || result.get(0).asInt(-1) != 0) {
            throw new MeshApException(MeshApException.Kind.PROTOCOL,
                    obj + "." + method + " trả trạng thái " + result);
        }
        return result.size() > 1 ? result.get(1) : objectMapper.createObjectNode();
    }

    private JsonNode callOptional(String host, String sid, String obj, String method, Map<String, Object> args) {
        try {
            return callRequired(host, sid, obj, method, args);
        } catch (MeshApException e) {
            log.debug("[MeshAp] {} {}.{} bỏ qua: {}", host, obj, method, e.getMessage());
            return null;
        }
    }

    private JsonNode rpc(String host, String sid, String obj, String method, Map<String, Object> args)
            throws MeshApException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("jsonrpc", "2.0");
        body.put("id", rpcId.incrementAndGet());
        body.put("method", "call");
        ArrayNode params = body.putArray("params");
        params.add(sid == null ? ANON_SESSION : sid);
        params.add(obj);
        params.add(method);
        params.add(objectMapper.valueToTree(args));

        HttpsURLConnection conn = null;
        try {
            conn = open(host, "/ubus");
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(body));
            }
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) {
                throw new MeshApException(MeshApException.Kind.PROTOCOL, "ubus trả HTTP " + code + " không có nội dung");
            }
            try (in) {
                return objectMapper.readTree(in);
            }
        } catch (IOException e) {
            throw new MeshApException(MeshApException.Kind.UNREACHABLE,
                    "Không kết nối được tới mesh " + host + ": " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private HttpsURLConnection open(String host, String path) throws IOException {
        HttpsURLConnection conn = (HttpsURLConnection) URI.create("https://" + host + path).toURL().openConnection();
        conn.setSSLSocketFactory(lanSocketFactory);
        conn.setHostnameVerifier((h, s) -> true);
        conn.setInstanceFollowRedirects(false);
        conn.setUseCaches(false);
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        return conn;
    }

    private static void drain(HttpsURLConnection conn) {
        try {
            InputStream in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in != null) try (in) { in.readAllBytes(); }
        } catch (IOException ignored) {
            // không cần nội dung trang
        }
    }

    /** Chỉ cho phép địa chỉ mạng nội bộ vì kết nối bỏ qua kiểm tra chứng chỉ. */
    static void ensurePrivateHost(String host) throws MeshApException {
        if (host == null || host.isBlank()) {
            throw new MeshApException(MeshApException.Kind.UNREACHABLE, "Thiết bị chưa có địa chỉ IP quản trị");
        }
        // Chấp nhận IPv4, IPv4:port (NAT qua VPN) hoặc IPv6 — không chấp nhận tên miền
        String ip = host;
        if (host.matches("[0-9.]+:[0-9]{1,5}")) {
            ip = host.substring(0, host.indexOf(':'));
        } else if (!host.matches("[0-9.]+|[0-9a-fA-F:]+")) {
            throw new MeshApException(MeshApException.Kind.UNREACHABLE,
                    "Địa chỉ mesh phải là IP nội bộ, không dùng tên miền: " + host);
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            if (!(addr.isSiteLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress())) {
                throw new MeshApException(MeshApException.Kind.UNREACHABLE,
                        "Chỉ giám sát mesh qua địa chỉ IP nội bộ (VPN/LAN), không qua Internet: " + host);
            }
        } catch (IOException e) {
            throw new MeshApException(MeshApException.Kind.UNREACHABLE, "Địa chỉ IP mesh không hợp lệ: " + host, e);
        }
    }

    private static SSLSocketFactory buildLanSocketFactory() {
        try {
            TrustManager[] trustAll = {new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }};
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            return ctx.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Không khởi tạo được TLS cho mesh nội bộ", e);
        }
    }
}
