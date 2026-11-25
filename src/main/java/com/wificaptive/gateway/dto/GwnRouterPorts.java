package com.wificaptive.gateway.dto;

import java.util.Map;

/**
 * Trạng thái link các cổng LAN của router Grandstream GWN (lấy từ /oapi/v1.0.0/device/info -> portInfo).
 * Mesh Viettel cắm cổng LAN nào thì rút dây là cổng đó link down NGAY (tầng vật lý) — tín hiệu on/off
 * nhanh & chắc hơn client/info (vốn giữ bản ghi client cũ khá lâu sau khi rút).
 *
 * key = số in trên vỏ cổng (silkScreenPort: "4","5","6"...), value = true nếu link up.
 */
public record GwnRouterPorts(boolean apiReachable, Map<String, Boolean> lanUpBySilk) {

    public static GwnRouterPorts unreachable() {
        return new GwnRouterPorts(false, Map.of());
    }

    public boolean hasLanInfo() {
        return !lanUpBySilk.isEmpty();
    }

    /** Có bất kỳ cổng LAN nào đang up (dùng cho site 1 mesh: cổng LAN up = mesh còn cắm). */
    public boolean anyLanUp() {
        return lanUpBySilk.values().stream().anyMatch(Boolean::booleanValue);
    }

    /** Link của đúng một cổng LAN theo số in trên vỏ (null nếu không biết cổng đó). */
    public Boolean isUp(String silk) {
        return silk == null ? null : lanUpBySilk.get(silk.trim());
    }
}
