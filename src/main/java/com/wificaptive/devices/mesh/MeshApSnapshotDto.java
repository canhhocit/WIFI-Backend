package com.wificaptive.devices.mesh;

import java.time.Instant;
import java.util.List;

/**
 * Ảnh chụp trạng thái một thiết bị mesh, đọc trực tiếp qua API ubus của thiết bị.
 * Không bao giờ chứa mật khẩu Wi-Fi hay mật khẩu quản trị.
 *
 * monitoringMode:
 *  LIVE           - đọc thành công từ thiết bị
 *  UNREACHABLE    - không kết nối được tới thiết bị
 *  AUTH_FAILED    - thiết bị từ chối đăng nhập
 *  ERROR          - lỗi khác khi đọc dữ liệu
 *  NOT_CONFIGURED - thiết bị chưa có địa chỉ IP quản trị
 *  DISABLED       - máy chủ này chưa bật giám sát mesh trực tiếp (không có đường mạng tới LAN điểm phát)
 */
public record MeshApSnapshotDto(
        Long deviceId,
        String host,
        String monitoringMode,
        String message,
        Instant fetchedAt,
        Instant lastSuccessAt,
        String serialNumber,
        String model,
        String firmwareVersion,
        String hostname,
        String osRelease,
        Long uptimeSeconds,
        Integer memoryUsagePercent,
        Double loadAverage1m,
        List<Radio> radios,
        List<Client> clients,
        /** FACTORY = đang dùng S/N gốc, MANAGED = mật khẩu quản lý chung */
        String credentialState
) {

    public record Radio(String band, Integer channel, List<Ssid> ssids) {}

    public record Ssid(String ssid, String bssid, String security, boolean hidden) {}

    public record Client(
            String mac,
            String ip,
            String hostname,
            String band,
            String ssid,
            String bssid,
            Integer linkSpeedMbps,
            Integer rssi,
            Long txKbps,
            Long rxKbps
    ) {}

    public boolean isLive() {
        return "LIVE".equals(monitoringMode);
    }

    public static MeshApSnapshotDto status(Long deviceId, String host, String mode, String message,
                                           Instant lastSuccessAt) {
        return new MeshApSnapshotDto(deviceId, host, mode, message, Instant.now(), lastSuccessAt,
                null, null, null, null, null, null, null, null, List.of(), List.of(), null);
    }

    public MeshApSnapshotDto withDevice(Long id, String hostAddr) {
        return new MeshApSnapshotDto(id, hostAddr, monitoringMode, message, fetchedAt, lastSuccessAt,
                serialNumber, model, firmwareVersion, hostname, osRelease, uptimeSeconds,
                memoryUsagePercent, loadAverage1m, radios, clients, credentialState);
    }

    public MeshApSnapshotDto withMessage(String msg) {
        return new MeshApSnapshotDto(deviceId, host, monitoringMode, msg, fetchedAt, lastSuccessAt,
                serialNumber, model, firmwareVersion, hostname, osRelease, uptimeSeconds,
                memoryUsagePercent, loadAverage1m, radios, clients, credentialState);
    }

    public MeshApSnapshotDto withCredentialState(String state) {
        return new MeshApSnapshotDto(deviceId, host, monitoringMode, message, fetchedAt, lastSuccessAt,
                serialNumber, model, firmwareVersion, hostname, osRelease, uptimeSeconds,
                memoryUsagePercent, loadAverage1m, radios, clients, state);
    }
}
