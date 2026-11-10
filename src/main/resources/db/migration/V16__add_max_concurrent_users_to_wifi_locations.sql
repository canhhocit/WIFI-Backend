-- ==============================================================================
-- V16: Bổ sung trường giới hạn số người truy cập đồng thời (max_concurrent_users)
-- ==============================================================================

ALTER TABLE wifi_locations 
ADD COLUMN IF NOT EXISTS max_concurrent_users INTEGER NOT NULL DEFAULT 50;

UPDATE wifi_locations 
SET max_concurrent_users = 50 
WHERE max_concurrent_users IS NULL;
