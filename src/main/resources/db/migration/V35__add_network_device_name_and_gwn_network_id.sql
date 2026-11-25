-- ==============================================================================
-- V34: Bổ sung tên gợi nhớ (name) và mã Network GWN Cloud tùy chọn (gwn_network_id)
--      cho bảng network_devices để hỗ trợ gán/quản lý thiết bị theo từng Nhà văn hóa
-- ==============================================================================

ALTER TABLE network_devices
    ADD COLUMN IF NOT EXISTS name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS gwn_network_id VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_network_devices_location_id ON network_devices(location_id);
CREATE INDEX IF NOT EXISTS idx_network_devices_type_location ON network_devices(device_type, location_id);
