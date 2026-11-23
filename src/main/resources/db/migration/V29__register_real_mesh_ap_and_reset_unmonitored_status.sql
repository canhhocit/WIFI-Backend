-- ==============================================================================
-- V29: Khai báo mesh AP Viettel HomeWiFi vAP-32X6V1 đang thực sự lắp tại điểm demo
--      (MAC 24:0B:2B:47:2A:22, IP tĩnh 192.168.80.111, cắm LAN4 của GWN7002).
--      Đưa trạng thái các AP chưa được giám sát về UNKNOWN thay vì ONLINE cố định,
--      để giao diện không hiển thị trạng thái giả. Trạng thái thật do module giám sát
--      mesh (app.mesh-ap.enabled=true) cập nhật khi đọc được thiết bị.
-- ==============================================================================

INSERT INTO network_devices (location_id, device_type, model, serial_number, mac_address, vendor, firmware_version, ip_address, status, installed_at, created_at, updated_at)
SELECT 1, 'ACCESS_POINT', 'vAP-32X6V1', '240B2B472A22', '24:0B:2B:47:2A:22', 'Viettel High Tech', 'V1.6.6_VN', '192.168.80.111', 'UNKNOWN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM network_devices WHERE mac_address = '24:0B:2B:47:2A:22')
  AND EXISTS (SELECT 1 FROM wifi_locations WHERE id = 1);

UPDATE network_devices
SET status = 'UNKNOWN', updated_at = CURRENT_TIMESTAMP
WHERE device_type IN ('ACCESS_POINT', 'AP')
  AND vendor = 'Viettel High Tech'
  AND status = 'ONLINE';

-- Trạng thái mật khẩu quản trị mesh: FACTORY = dùng S/N gốc in trên nhãn,
-- MANAGED = hệ thống đã đổi sang mật khẩu quản lý chung (app.mesh-ap.managed-password)
ALTER TABLE network_devices ADD COLUMN mesh_credential_state VARCHAR(20) DEFAULT 'FACTORY';
ALTER TABLE network_devices ADD COLUMN mesh_password_rotated_at TIMESTAMP WITH TIME ZONE;
