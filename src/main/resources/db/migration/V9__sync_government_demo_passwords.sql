-- ==============================================================================
-- V9: Synchronize & activate password hash for government demo accounts
-- ==============================================================================

UPDATE admin_users
SET failed_login_attempts = 0,
    locked_until = NULL,
    enabled = TRUE
WHERE username IN ('admin', 'sotttt_admin', 'hongbang_officer', 'vnpt_tech01');
