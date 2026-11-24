-- ==============================================================================
-- V31: Khai báo lại mesh AP Viettel HomeWiFi vAP-32X6V1 thật tại điểm demo.
--      Sau V29 (insert) và V30 (update), hàng AP (MAC 24:0B:2B:47:2A:22) đã bị mất
--      khỏi network_devices (thao tác nhập/xóa dữ liệu trước đó). Flyway không chạy
--      lại V29 nên AP không được tạo lại -> /targets rỗng, agent không khớp được AP.
--      Migration này upsert lại AP một cách idempotent để mesh tự đẩy dữ liệu (self-push)
--      và module giám sát nhận diện được thiết bị theo MAC.
-- ==============================================================================

-- Tạo lại nếu đang thiếu (chỉ khi có location id=1 để thỏa khóa ngoại)
INSERT INTO network_devices (location_id, device_type, model, serial_number, mac_address, vendor, firmware_version, ip_address, status, installed_at, created_at, updated_at)
SELECT 1, 'ACCESS_POINT', 'vAP-32X6V1', '240B2B472A22', '24:0B:2B:47:2A:22', 'Viettel High Tech', 'V1.6.6_VN', '192.168.80.111', 'UNKNOWN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM network_devices WHERE mac_address = '24:0B:2B:47:2A:22')
  AND EXISTS (SELECT 1 FROM wifi_locations WHERE id = 1);

-- Chuẩn hóa thông tin AP thật (áp dụng kể cả khi hàng đã tồn tại sẵn)
UPDATE network_devices
SET device_type = 'ACCESS_POINT', model = 'vAP-32X6V1', vendor = 'Viettel High Tech',
    firmware_version = 'V1.6.6_VN', ip_address = '192.168.80.111', updated_at = CURRENT_TIMESTAMP
WHERE mac_address = '24:0B:2B:47:2A:22';
