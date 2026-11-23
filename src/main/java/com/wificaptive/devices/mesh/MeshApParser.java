package com.wificaptive.devices.mesh;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chuyển dữ liệu thô ubus của mesh vAP-32X6V1 (firmware V1.6.6_VN) thành {@link MeshApSnapshotDto}.
 * Bỏ qua mọi trường mật khẩu (Pass-phrase, wpapsk...) — không bao giờ đưa ra ngoài.
 */
public final class MeshApParser {

    private MeshApParser() {}

    /**
     * @param topologyRoot  JSON đã parse từ chuỗi luaTopologyInfo của dat2wifi.getTopologyDump
     * @param systemInfo    kết quả system.info (có thể null)
     * @param board         kết quả system.board (có thể null)
     * @param hostHints     kết quả luci-rpc.getHostHints, dạng {MAC: {ipaddrs:[], name}} (có thể null)
     * @param signalByMac   RSSI theo MAC lấy từ iwinfo.assoclist (có thể rỗng)
     */
    public static MeshApSnapshotDto parse(JsonNode topologyRoot, JsonNode systemInfo, JsonNode board,
                                          JsonNode hostHints, Map<String, Integer> signalByMac) {
        JsonNode dev = firstTopologyEntry(topologyRoot);

        String serial = text(dev, "Serial number");
        String model = text(dev, "Product class");
        String firmware = text(dev, "Firmware version");

        List<MeshApSnapshotDto.Radio> radios = new ArrayList<>();
        List<MeshApSnapshotDto.Client> clients = new ArrayList<>();

        for (JsonNode radio : iterable(dev.path("Radio Info"))) {
            Integer channel = radio.hasNonNull("channel") ? radio.path("channel").asInt() : null;
            String radioBand = bandFromChannel(channel);
            List<MeshApSnapshotDto.Ssid> ssids = new ArrayList<>();

            for (JsonNode bss : iterable(radio.path("BSSINFO"))) {
                String ssid = text(bss, "SSID");
                String bssid = upperMac(text(bss, "BSSID"));
                ssids.add(new MeshApSnapshotDto.Ssid(ssid, bssid, securityLabel(text(bss, "Security")),
                        bss.path("Hidden").asInt(0) == 1));

                for (JsonNode sta : iterable(bss.path("connected sta info"))) {
                    if ("Yes".equalsIgnoreCase(text(sta, "BH STA"))) {
                        continue; // liên kết backhaul giữa các mesh, không phải người dùng
                    }
                    String mac = upperMac(text(sta, "STA MAC address"));
                    if (mac == null) continue;
                    String band = mediumToBand(text(sta, "Medium"), radioBand);
                    Integer rssi = parseIntOrNull(text(sta, "uplink rssi"));
                    if (rssi == null && signalByMac != null) {
                        rssi = signalByMac.get(mac);
                    }
                    String ip = null;
                    String hostname = null;
                    JsonNode hint = hostHints != null ? findIgnoreCase(hostHints, mac) : null;
                    if (hint != null) {
                        JsonNode ips = hint.path("ipaddrs");
                        if (ips.isArray() && ips.size() > 0) ip = ips.get(0).asText();
                        hostname = text(hint, "name");
                    }
                    clients.add(new MeshApSnapshotDto.Client(
                            mac, ip, hostname, band, ssid, bssid,
                            parseIntOrNull(text(sta, "uplink speed")),
                            rssi,
                            sta.hasNonNull("Tp Tx Dl KBps") ? sta.path("Tp Tx Dl KBps").asLong() : null,
                            sta.hasNonNull("Tp Rx Ul KBps") ? sta.path("Tp Rx Ul KBps").asLong() : null));
                }
            }
            radios.add(new MeshApSnapshotDto.Radio(radioBand, channel, ssids));
        }

        Long uptime = null;
        Integer memPct = null;
        Double load1 = null;
        if (systemInfo != null && !systemInfo.isMissingNode()) {
            if (systemInfo.hasNonNull("uptime")) uptime = systemInfo.path("uptime").asLong();
            JsonNode mem = systemInfo.path("memory");
            long total = mem.path("total").asLong(0);
            long avail = mem.has("available") ? mem.path("available").asLong(0) : mem.path("free").asLong(0);
            if (total > 0) memPct = (int) Math.round((total - avail) * 100.0 / total);
            JsonNode load = systemInfo.path("load");
            if (load.isArray() && load.size() > 0) {
                load1 = Math.round(load.get(0).asLong() / 65536.0 * 100.0) / 100.0;
            }
        }

        String hostname = board != null ? text(board, "hostname") : null;
        String release = board != null ? text(board.path("release"), "description") : null;
        if (model == null && board != null) model = text(board, "model");

        Instant now = Instant.now();
        return new MeshApSnapshotDto(null, null, "LIVE", "Đọc trực tiếp từ thiết bị mesh thành công",
                now, now, serial, model, firmware, hostname, release, uptime, memPct, load1,
                List.copyOf(radios), List.copyOf(clients), null);
    }

    static JsonNode firstTopologyEntry(JsonNode root) {
        if (root == null) return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        JsonNode arr = root.path("topology information");
        if (arr.isArray() && arr.size() > 0) return arr.get(0);
        return root;
    }

    static String bandFromChannel(Integer channel) {
        if (channel == null || channel <= 0) return null;
        return channel <= 14 ? "2.4GHz" : "5GHz";
    }

    static String mediumToBand(String medium, String fallback) {
        if (medium == null) return fallback;
        String m = medium.toUpperCase(Locale.ROOT);
        if (m.contains("5")) return "5GHz";
        if (m.contains("2")) return "2.4GHz";
        if (m.contains("6")) return "6GHz";
        return fallback;
    }

    /** Mã xác thực WSC (bitmask) của MediaTek: 0x0001 Open, 0x0002 WPA-PSK, 0x0020 WPA2-PSK, 0x0040 WPA3-SAE. */
    static String securityLabel(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int v = Integer.parseInt(raw.trim(), 16);
            List<String> parts = new ArrayList<>();
            if ((v & 0x0040) != 0) parts.add("WPA3");
            if ((v & 0x0020) != 0) parts.add("WPA2");
            if ((v & 0x0002) != 0) parts.add("WPA");
            if (!parts.isEmpty()) return String.join("/", parts);
            if ((v & 0x0001) != 0) return "OPEN";
            return "0x" + raw.trim();
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("NA") || t.equalsIgnoreCase("N/A")) return null;
        try {
            return (int) Math.round(Double.parseDouble(t));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String upperMac(String mac) {
        if (mac == null || mac.isBlank()) return null;
        return mac.trim().toUpperCase(Locale.ROOT);
    }

    private static JsonNode findIgnoreCase(JsonNode obj, String key) {
        if (obj.has(key)) return obj.get(key);
        Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            if (e.getKey().equalsIgnoreCase(key)) return e.getValue();
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        String s = v.asText();
        return s.isBlank() ? null : s;
    }

    private static Iterable<JsonNode> iterable(JsonNode node) {
        return node != null && node.isArray() ? node : List.of();
    }
}
