package com.wificaptive.devices.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Điểm nhận dữ liệu từ agent thu thập đặt trong LAN của điểm phát.
 *
 * Máy chủ trung tâm thường không vào được LAN sau NAT của từng điểm, nên một agent tại chỗ đăng nhập
 * mesh, đọc các lệnh ubus thô rồi gửi lên đây; máy chủ dùng cùng bộ phân tích {@link MeshApParser}.
 *
 * Xác thực bằng token agent: cấu hình chỉ lưu SHA-256 của token ({@code app.mesh-ap.agent-token-sha256}),
 * không lưu token gốc. Để trống = tắt endpoint.
 */
@RestController
@RequestMapping("/api/v1/agent/mesh")
public class MeshAgentController {

    private static final Logger log = LoggerFactory.getLogger(MeshAgentController.class);
    static final String TOKEN_HEADER = "X-Agent-Token";

    private final MeshApMonitorService monitor;
    private final NetworkDeviceRepository repo;
    private final ObjectMapper om;
    private final byte[] expectedHash;
    private final boolean autoProvisionEnabled;

    public MeshAgentController(MeshApMonitorService monitor, NetworkDeviceRepository repo, ObjectMapper om,
                               @Value("${app.mesh-ap.agent-token-sha256:}") String tokenSha256,
                               @Value("${app.mesh-ap.agent-auto-provision:true}") boolean autoProvisionEnabled) {
        this.monitor = monitor;
        this.repo = repo;
        this.om = om;
        this.expectedHash = parseHex(tokenSha256);
        this.autoProvisionEnabled = autoProvisionEnabled;
    }

    @GetMapping("/targets")
    public ResponseEntity<?> targets(@RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        if (!authorized(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<Map<String, Object>> out = new ArrayList<>();
        for (NetworkDevice d : monitor.agentTargets()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mac", d.getMacAddress());
            m.put("host", d.getIpAddress().trim());
            out.add(m);
        }
        return ResponseEntity.ok(Map.of("targets", out));
    }

    /**
     * Body: {"agentId": "...", "devices": [{"mac", "host", "mode": "LIVE|UNREACHABLE|AUTH_FAILED|ERROR",
     * "message", "topology": chuỗi luaTopologyInfo, "systemInfo", "board", "hostHints", "signals": {MAC: dBm}}]}
     */
    @PostMapping("/snapshots")
    public ResponseEntity<?> snapshots(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                       @RequestBody JsonNode body) {
        if (!authorized(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String agentId = body.path("agentId").asText("agent");
        int accepted = 0;
        List<String> unknown = new ArrayList<>();
        for (JsonNode item : body.path("devices")) {
            MeshApSnapshotDto snapshot = toSnapshot(item);
            Optional<NetworkDevice> dev = findDevice(item, snapshot);
            if (dev.isEmpty()) dev = autoProvision(item, snapshot);
            if (dev.isEmpty()) {
                unknown.add(item.path("mac").asText(""));
                continue;
            }
            if (snapshot.isLive()) {
                snapshot = snapshot.withMessage("Mesh đọc thành công qua agent " + agentId);
            }
            monitor.ingestFromAgent(dev.get(), snapshot);
            accepted++;
        }
        log.info("[MeshAgent] {} gửi {} AP ({} không khớp)", agentId, accepted, unknown.size());
        return ResponseEntity.ok(Map.of("accepted", accepted, "unknown", unknown));
    }

    /**
     * Khớp thiết bị: MAC (khai báo trong danh sách bàn giao) -> S/N mesh tự báo -> IP quản trị
     * (chỉ khi duy nhất một mesh AP dùng IP đó, vì các điểm có thể trùng dải LAN).
     */
    private Optional<NetworkDevice> findDevice(JsonNode item, MeshApSnapshotDto snapshot) {
        String mac = normalizeMac(item.path("mac").asText(""));
        Optional<NetworkDevice> dev = mac == null ? Optional.empty() : repo.findByMacAddress(mac);
        if (dev.isEmpty() && StringUtils.hasText(snapshot.serialNumber())) {
            dev = repo.findBySerialNumber(snapshot.serialNumber().trim());
        }
        if (dev.isEmpty()) {
            String host = item.path("host").asText("").trim();
            if (!host.isEmpty()) {
                List<NetworkDevice> sameIp = monitor.agentTargets().stream()
                        .filter(d -> host.equals(d.getIpAddress().trim())).toList();
                if (sameIp.size() == 1) dev = Optional.of(sameIp.get(0));
            }
        }
        return dev.filter(MeshApMonitorService::isMeshAp);
    }

    /**
     * Mesh chưa có trong CSDL -> tạo bản ghi từ chính dữ liệu thiết bị tự báo (MAC/S-N/model/IP),
     * thay cho việc seed cứng. Nhờ vậy mỗi điểm phát chỉ cần cài agent self-push là AP tự xuất hiện,
     * không phải khai báo tay từng điểm. Chỉ tạo với ảnh chụp LIVE và MAC hợp lệ (endpoint đã có token
     * bảo vệ, và chỉ agent tại mesh mới gửi được dữ liệu này).
     */
    private Optional<NetworkDevice> autoProvision(JsonNode item, MeshApSnapshotDto snapshot) {
        // Chỉ tạo khi là ảnh chụp LIVE thật và mesh có báo S/N trong topology (tránh tạo thiết bị rác
        // từ payload chỉ có MAC). Mesh Viettel vAP luôn báo "Serial number" khi đọc được.
        if (!autoProvisionEnabled || !snapshot.isLive() || !StringUtils.hasText(snapshot.serialNumber())) {
            return Optional.empty();
        }
        String mac = normalizeMac(item.path("mac").asText(""));
        if (mac == null) return Optional.empty();
        String serial = snapshot.serialNumber().trim();
        String host = item.path("host").asText("").trim();
        try {
            NetworkDevice d = new NetworkDevice();
            d.setDeviceType("ACCESS_POINT");
            d.setMacAddress(mac);
            d.setSerialNumber(serial);
            d.setModel(StringUtils.hasText(snapshot.model()) ? snapshot.model() : "vAP-32X6V1");
            d.setVendor("Viettel High Tech");
            d.setFirmwareVersion(snapshot.firmwareVersion());
            if (!host.isEmpty()) d.setIpAddress(host);
            d.setStatus("UNKNOWN");
            Instant now = Instant.now();
            d.setInstalledAt(now);
            d.setCreatedAt(now);
            d.setUpdatedAt(now);
            NetworkDevice saved = repo.save(d);
            log.info("[MeshAgent] Tự đăng ký mesh AP mới từ self-push: mac={} serial={} ip={}", mac, serial, host);
            return Optional.ofNullable(saved);
        } catch (DataIntegrityViolationException e) {
            // Hai lần đẩy chạy song song cùng tạo một thiết bị -> lấy lại bản ghi vừa được tạo.
            Optional<NetworkDevice> again = repo.findByMacAddress(mac);
            if (again.isEmpty()) again = repo.findBySerialNumber(serial);
            return again.filter(MeshApMonitorService::isMeshAp);
        }
    }

    MeshApSnapshotDto toSnapshot(JsonNode item) {
        String host = item.path("host").asText(null);
        String mode = item.path("mode").asText("LIVE").toUpperCase(Locale.ROOT);
        if (!"LIVE".equals(mode)) {
            if (!Set.of("UNREACHABLE", "AUTH_FAILED", "ERROR").contains(mode)) mode = "ERROR";
            return MeshApSnapshotDto.status(null, host, mode, item.path("message").asText("Agent không đọc được thiết bị"), null);
        }
        try {
            JsonNode topoNode = item.path("topology");
            // Agent trên PC gửi chuỗi luaTopologyInfo; script chạy trên mesh gửi nguyên kết quả getTopologyDump
            if (topoNode.isObject() && topoNode.path("luaTopologyInfo").isTextual()) {
                topoNode = topoNode.path("luaTopologyInfo");
            }
            JsonNode topology = topoNode.isTextual() ? om.readTree(topoNode.asText()) : topoNode;
            Map<String, Integer> signals = new HashMap<>();
            item.path("signals").fields().forEachRemaining(e -> {
                String m = normalizeMac(e.getKey());
                if (m != null && e.getValue().isNumber()) signals.put(m, e.getValue().asInt());
            });
            // Kết quả iwinfo.assoclist thô theo interface: {"rax0": {"results": [{"mac","signal"}]}}
            item.path("assoc").fields().forEachRemaining(e -> {
                for (JsonNode sta : e.getValue().path("results")) {
                    String m = normalizeMac(sta.path("mac").asText(""));
                    if (m != null && sta.path("signal").isNumber() && sta.path("signal").asInt() < 0) {
                        signals.putIfAbsent(m, sta.path("signal").asInt());
                    }
                }
            });
            MeshApSnapshotDto s = MeshApParser.parse(topology, nullIfMissing(item.path("systemInfo")),
                    nullIfMissing(item.path("board")), nullIfMissing(item.path("hostHints")), signals);
            return s.withDevice(null, host);
        } catch (Exception e) {
            return MeshApSnapshotDto.status(null, host, "ERROR", "Dữ liệu agent không hợp lệ: " + e.getMessage(), null);
        }
    }

    private static JsonNode nullIfMissing(JsonNode n) {
        return n == null || n.isMissingNode() || n.isNull() ? null : n;
    }

    private boolean authorized(String token) {
        if (expectedHash == null || !StringUtils.hasText(token)) return false;
        try {
            byte[] got = MessageDigest.getInstance("SHA-256").digest(token.trim().getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(got, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] parseHex(String hex) {
        if (!StringUtils.hasText(hex)) return null;
        String h = hex.trim().toLowerCase(Locale.ROOT);
        if (!h.matches("[0-9a-f]{64}")) {
            log.warn("[MeshAgent] app.mesh-ap.agent-token-sha256 không phải SHA-256 hex -> tắt endpoint agent");
            return null;
        }
        return HexFormat.of().parseHex(h);
    }

    static String normalizeMac(String raw) {
        String hex = raw == null ? "" : raw.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
        if (hex.length() != 12) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i += 2) {
            if (i > 0) sb.append(':');
            sb.append(hex, i, i + 2);
        }
        return sb.toString();
    }
}
