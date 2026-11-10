-- ==============================================================================
-- V15: Xóa các AP mẫu cũ (IDs 4, 5, 6) bị trùng lặp, chuẩn hóa 3 AP thực tế
-- ==============================================================================

-- 1. Xóa các chỉ số telemetry của các AP mẫu cũ
DELETE FROM device_telemetry_metrics 
WHERE device_id IN (4, 5, 6);

-- 2. Xóa các AP mẫu cũ (MAC 24:0B:2B:46:43:B1, 24:0B:2B:46:7A:83, 24:0B:2B:46:4F:EF)
DELETE FROM network_devices 
WHERE id IN (4, 5, 6);

-- 3. Chuẩn hóa tên vendor và trạng thái hoạt động cho 3 AP Viettel High Tech thực tế
UPDATE network_devices 
SET status = 'ONLINE', 
    vendor = 'Viettel High Tech', 
    last_seen_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE device_type = 'ACCESS_POINT';

