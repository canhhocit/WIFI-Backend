-- ==============================================================================
-- V13: Đồng bộ toàn bộ nhà mạng cung cấp sang Viettel
-- ==============================================================================

UPDATE wifi_locations
SET isp_name = 'Viettel'
WHERE isp_name IS NULL OR isp_name != 'Viettel';

