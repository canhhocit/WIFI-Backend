-- ==============================================================================
-- V17: Loại bỏ nhầm lẫn thiết bị test di động khỏi danh mục hạ tầng network_devices
-- Chuẩn hóa 3 trạm phát sóng Wi-Fi 6 Viettel vAP-32X6V1 thực tế
-- ==============================================================================

-- 1. Xóa telemetry metrics và các bản ghi nhầm lẫn thiết bị máy khách (IDs 26, 27, 28, 32 hoặc theo MAC khách)
DELETE FROM device_telemetry_metrics WHERE device_id IN (SELECT id FROM network_devices WHERE mac_address IN ('AA:0C:60:B8:7A:4C', '96:FD:0E:CA:CA:BD', 'EA:73:15:58:63:91'));
DELETE FROM network_devices WHERE mac_address IN ('AA:0C:60:B8:7A:4C', '96:FD:0E:CA:CA:BD', 'EA:73:15:58:63:91');

-- 2. Đảm bảo 3 trạm phát sóng Viettel High Tech chính thức tồn tại trong danh mục hạ tầng
INSERT INTO network_devices (location_id, device_type, model, serial_number, mac_address, vendor, firmware_version, ip_address, status, installed_at, created_at, updated_at)
SELECT 1, 'ACCESS_POINT', 'vAP-32X6V1', '240B2B4643B1', '24:0B:2B:46:43:B1', 'Viettel High Tech', '1.2.0', '192.168.80.112', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM network_devices WHERE mac_address = '24:0B:2B:46:43:B1');

INSERT INTO network_devices (location_id, device_type, model, serial_number, mac_address, vendor, firmware_version, ip_address, status, installed_at, created_at, updated_at)
SELECT 1, 'ACCESS_POINT', 'vAP-32X6V1', '240B2B467A83', '24:0B:2B:46:7A:83', 'Viettel High Tech', '1.2.0', '192.168.80.168', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM network_devices WHERE mac_address = '24:0B:2B:46:7A:83');

INSERT INTO network_devices (location_id, device_type, model, serial_number, mac_address, vendor, firmware_version, ip_address, status, installed_at, created_at, updated_at)
SELECT 1, 'ACCESS_POINT', 'vAP-32X6V1', '240B2B464FEF', '24:0B:2B:46:4F:EF', 'Viettel High Tech', '1.2.0', '192.168.80.200', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM network_devices WHERE mac_address = '24:0B:2B:46:4F:EF');
