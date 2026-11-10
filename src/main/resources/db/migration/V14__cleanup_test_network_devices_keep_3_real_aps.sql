-- ==============================================================================
-- V14: Dọn dẹp thiết bị test, chỉ giữ lại đúng 1 Router GWN7002 và 3 AP vAP-32X6V1 thực tế
-- ==============================================================================

DELETE FROM device_telemetry_metrics 
WHERE device_id IN (16, 18, 19, 20, 21, 22, 23, 24, 25);

DELETE FROM network_devices 
WHERE id IN (16, 18, 19, 20, 21, 22, 23, 24, 25);

UPDATE network_devices 
SET status = 'ONLINE', vendor = 'Viettel', last_seen_at = CURRENT_TIMESTAMP 
WHERE id IN (4, 5, 6);

