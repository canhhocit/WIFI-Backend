-- ==============================================================================
-- V26: Bắt buộc đổi mật khẩu lần đầu cho tài khoản cán bộ quản trị (future-update.md)
-- ==============================================================================

ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Cập nhật tài khoản cán bộ cấp cơ sở mới cần đổi mật khẩu lần đầu để trải nghiệm
UPDATE admin_users SET must_change_password = TRUE WHERE username = 'canbo_hongbang';
