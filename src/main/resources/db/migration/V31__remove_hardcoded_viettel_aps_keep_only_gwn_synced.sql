-- ==============================================================================
-- V31: Xóa các bản ghi Access Point Viettel được code cứng/seed thủ công trong DB.
--      Chỉ giữ lại thiết bị thực tế đồng bộ được từ Grandstream Cloud (nếu GWN
--      không lấy được AP nào thì số lượng AP hiển thị trung thực là 0).
-- ==============================================================================

DELETE FROM device_telemetry_metrics
WHERE device_id IN (
    SELECT id FROM network_devices
    WHERE device_type IN ('ACCESS_POINT', 'AP')
      AND (mac_address = '24:0B:2B:47:2A:22' OR vendor = 'Viettel High Tech')
);

UPDATE incident_tickets SET device_id = NULL
WHERE device_id IN (
    SELECT id FROM network_devices
    WHERE device_type IN ('ACCESS_POINT', 'AP')
      AND (mac_address = '24:0B:2B:47:2A:22' OR vendor = 'Viettel High Tech')
);

UPDATE device_swap_logs SET old_device_id = NULL
WHERE old_device_id IN (
    SELECT id FROM network_devices
    WHERE device_type IN ('ACCESS_POINT', 'AP')
      AND (mac_address = '24:0B:2B:47:2A:22' OR vendor = 'Viettel High Tech')
);

DELETE FROM device_swap_logs
WHERE new_device_id IN (
    SELECT id FROM network_devices
    WHERE device_type IN ('ACCESS_POINT', 'AP')
      AND (mac_address = '24:0B:2B:47:2A:22' OR vendor = 'Viettel High Tech')
);

DELETE FROM network_devices
WHERE device_type IN ('ACCESS_POINT', 'AP')
  AND (mac_address = '24:0B:2B:47:2A:22' OR vendor = 'Viettel High Tech');
