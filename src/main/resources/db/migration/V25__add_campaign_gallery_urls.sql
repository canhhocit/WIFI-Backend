-- ==============================================================================
-- V25: Bổ sung thư viện nhiều ảnh (Poster & Gallery) cho Chiến dịch truyền thông
-- ==============================================================================

ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS gallery_urls TEXT;
