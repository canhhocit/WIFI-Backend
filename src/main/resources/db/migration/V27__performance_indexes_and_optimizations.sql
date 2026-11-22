-- ==============================================================================
-- V27: Database Performance Indexes for High-Concurrency Captive Portal Traffic
-- Optimizes active session count, device lookup, and audit trail queries
-- ==============================================================================

-- 1. Composite index for Active CCU Count (Used by getActiveSessionCount & countActiveSessionsByLocation)
-- Query: WHERE location_id = ? AND status = 'AUTHORIZED' AND expires_at > ?
CREATE INDEX IF NOT EXISTS idx_guest_sessions_active_ccu
ON guest_sessions (location_id, status, expires_at);

-- 2. Composite index for Session Expiration Passivation (Used by SessionCleanupScheduler)
-- Query: WHERE status = :status AND expires_at <= :now
CREATE INDEX IF NOT EXISTS idx_guest_sessions_status_expires_le
ON guest_sessions (status, expires_at);

-- 3. Composite index for 30-Day One-Click Remembered Devices Bypass
-- Query: WHERE client_mac = :mac AND bypass_otp_until > :now
CREATE INDEX IF NOT EXISTS idx_remembered_mac_bypass_until
ON remembered_devices (client_mac, bypass_otp_until);

-- 4. Composite index for Network Devices by Location and Device Type
-- Query: WHERE location_id = :locationId AND device_type = :deviceType
CREATE INDEX IF NOT EXISTS idx_network_devices_loc_type
ON network_devices (location_id, device_type);

-- 5. Composite index for Device Status Monitoring
-- Query: WHERE device_type = :deviceType AND status = :status
CREATE INDEX IF NOT EXISTS idx_network_devices_type_status
ON network_devices (device_type, status);

-- 6. Index for Citizen Issue Reports Lookup by Location
CREATE INDEX IF NOT EXISTS idx_citizen_reports_location_status
ON citizen_wifi_issue_reports (location_id, status, created_at DESC);
