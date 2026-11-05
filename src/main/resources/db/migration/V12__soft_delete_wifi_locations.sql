-- V12: Soft delete support for Wi-Fi locations to preserve guest session logs and legal compliance
ALTER TABLE wifi_locations ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE wifi_locations ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_wifi_locations_deleted ON wifi_locations(is_deleted);
