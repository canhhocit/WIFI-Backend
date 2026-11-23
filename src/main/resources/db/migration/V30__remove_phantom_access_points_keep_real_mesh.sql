-- ==============================================================================
-- V30: Xóa 3 AP seed không tồn tại thực tế (192.168.80.112 / .168 / .200 — không phản hồi
--      trên mạng điểm demo). Chỉ giữ mesh Viettel HomeWiFi vAP-32X6V1 đang thật sự kết nối
--      (MAC 24:0B:2B:47:2A:22, IP tĩnh 192.168.80.111, cắm LAN4 của GWN7002).
-- ==============================================================================

-- Các bảng tham chiếu tới thiết bị bị xóa
DELETE FROM device_telemetry_metrics
WHERE device_id IN (SELECT id FROM network_devices
                    WHERE mac_address IN ('24:0B:2B:46:43:B1', '24:0B:2B:46:7A:83', '24:0B:2B:46:4F:EF'));

UPDATE incident_tickets SET device_id = NULL
WHERE device_id IN (SELECT id FROM network_devices
                    WHERE mac_address IN ('24:0B:2B:46:43:B1', '24:0B:2B:46:7A:83', '24:0B:2B:46:4F:EF'));

UPDATE device_swap_logs SET old_device_id = NULL
WHERE old_device_id IN (SELECT id FROM network_devices
                        WHERE mac_address IN ('24:0B:2B:46:43:B1', '24:0B:2B:46:7A:83', '24:0B:2B:46:4F:EF'));

DELETE FROM device_swap_logs
WHERE new_device_id IN (SELECT id FROM network_devices
                        WHERE mac_address IN ('24:0B:2B:46:43:B1', '24:0B:2B:46:7A:83', '24:0B:2B:46:4F:EF'));

DELETE FROM network_devices
WHERE mac_address IN ('24:0B:2B:46:43:B1', '24:0B:2B:46:7A:83', '24:0B:2B:46:4F:EF');

-- Đảm bảo thông tin AP thật đúng với thiết bị (đọc trực tiếp từ mesh ngày 23/11/2026)
UPDATE network_devices
SET device_type = 'ACCESS_POINT', model = 'vAP-32X6V1', vendor = 'Viettel High Tech',
    firmware_version = 'V1.6.6_VN', ip_address = '192.168.80.111', updated_at = CURRENT_TIMESTAMP
WHERE mac_address = '24:0B:2B:47:2A:22';
