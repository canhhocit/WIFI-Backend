-- ==============================================================================
-- V18: Loại bỏ triệt để MAC điện thoại test khách khỏi danh mục hạ tầng network_devices
-- Đảm bảo chỉ có Router GWN7002 và 3 Access Point Viettel thực tế tồn tại
-- ==============================================================================

-- 1. Xóa các chỉ số telemetry phát sinh nhầm cho MAC điện thoại iPhone test
DELETE FROM device_telemetry_metrics 
WHERE device_id IN (
    SELECT id FROM network_devices 
    WHERE mac_address IN ('AA:0C:60:B8:7A:4C', '96:FD:0E:CA:CA:BD', 'EA:73:15:58:63:91')
       OR serial_number IN ('AA0C60B87A4C', '96FD0ECACABD', 'EA7315586391')
);

-- 2. Xóa triệt để khỏi bảng network_devices
DELETE FROM network_devices 
WHERE mac_address IN ('AA:0C:60:B8:7A:4C', '96:FD:0E:CA:CA:BD', 'EA:73:15:58:63:91')
   OR serial_number IN ('AA0C60B87A4C', '96FD0ECACABD', 'EA7315586391');
