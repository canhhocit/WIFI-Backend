-- ==============================================================================
-- V8: Administrative Users (5-tier roles & District assignments), Account Lockout, 
--     DNS Domain Filtering (Blocklist/Whitelist), and System Settings
-- ==============================================================================

-- 1. Alter admin_users table for 5-tier government roles, district assignment & lockout
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS admin_unit_id BIGINT;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITH TIME ZONE;

-- Add constraint to administrative units if exists
CREATE INDEX IF NOT EXISTS idx_admin_users_admin_unit ON admin_users(admin_unit_id);

-- Update role constraint or add sample users for 5-tier roles
-- Default password: password123 (BCrypt: $2a$10$wN10hX7kR00XpT2vj6sQ5eC9k0mHhLhD8lM7qY1vH5jN8pA0kO.4a)
-- Roles: ROLE_SUPER_ADMIN, ROLE_SOTTTT_ADMIN, ROLE_DISTRICT_OFFICER, ROLE_COMMUNE_OFFICER, ROLE_VNPT_ENGINEER

INSERT INTO admin_users (username, password_hash, email, full_name, role, enabled, phone_number, created_at, updated_at)
VALUES 
('sotttt_admin', '$2a$10$58V6n1sB/w64lQ7H4nQ0ue4xT57k0J1sUqX8xG5X7z0W.hQ.hQ.hQ', 'sotttt@haiphong.gov.vn', 'Cán bộ Sở TT&TT Hải Phòng', 'ROLE_SOTTTT_ADMIN', TRUE, '0901234567', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('hongbang_officer', '$2a$10$58V6n1sB/w64lQ7H4nQ0ue4xT57k0J1sUqX8xG5X7z0W.hQ.hQ.hQ', 'canbo.hongbang@haiphong.gov.vn', 'Cán bộ UBND Quận Hồng Bàng', 'ROLE_DISTRICT_OFFICER', TRUE, '0912345678', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('vnpt_tech01', '$2a$10$58V6n1sB/w64lQ7H4nQ0ue4xT57k0J1sUqX8xG5X7z0W.hQ.hQ.hQ', 'kythuat.vnpt01@vnpt.vn', 'Kỹ thuật viên Viễn thông VNPT', 'ROLE_VNPT_ENGINEER', TRUE, '0987654321', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. DNS Domain Rules (Screen B10: Bộ lọc tên miền DNS chặn nội dung độc hại & Whitelist Dịch vụ công)
CREATE TABLE IF NOT EXISTS dns_domain_rules (
    id BIGSERIAL PRIMARY KEY,
    domain_name VARCHAR(255) NOT NULL UNIQUE,
    rule_type VARCHAR(20) NOT NULL, -- BLOCKLIST, WHITELIST
    category VARCHAR(100), -- GAMBLING, ADULT, MALWARE, GOV_PORTAL, PUBLIC_SERVICE
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dns_rule_type ON dns_domain_rules(rule_type);
CREATE INDEX IF NOT EXISTS idx_dns_domain_name ON dns_domain_rules(domain_name);

-- Seed DNS Domain Filter rules
INSERT INTO dns_domain_rules (domain_name, rule_type, category, description, active)
VALUES 
('haiphong.gov.vn', 'WHITELIST', 'GOV_PORTAL', 'Cổng Thông tin Điện tử Thành phố Hải Phòng (Miễn phí & Ưu tiên)', TRUE),
('dichvucong.haiphong.gov.vn', 'WHITELIST', 'PUBLIC_SERVICE', 'Hệ thống Thông tin Giải quyết Thủ tục Hành chính Hải Phòng', TRUE),
('dichvucong.gov.vn', 'WHITELIST', 'PUBLIC_SERVICE', 'Cổng Dịch vụ công Quốc gia', TRUE),
('bet88.com', 'BLOCKLIST', 'GAMBLING', 'Trang web cờ bạc, cá độ trực tuyến trái phép', TRUE),
('kubet.com', 'BLOCKLIST', 'GAMBLING', 'Máy chủ cá cược cờ bạc bị cấm', TRUE),
('malware-sample-test.vn', 'BLOCKLIST', 'MALWARE', 'Tên miền phát tán mã độc lừa đảo người dân', TRUE);

-- 3. System Configuration Settings (Screen C06: Cấu hình hệ thống chung)
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description VARCHAR(255),
    category VARCHAR(50) DEFAULT 'GENERAL',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed Default System Parameters
INSERT INTO system_settings (setting_key, setting_value, description, category)
VALUES 
('ntp_server', 'vn.pool.ntp.org', 'Máy chủ đồng bộ thời gian chuẩn quốc gia NTP', 'NETWORK'),
('session_scan_interval_sec', '60', 'Chu kỳ máy quét dọn dẹp phiên hết hạn (giây)', 'SESSION'),
('max_failed_logins', '5', 'Số lần nhập sai mật khẩu tối đa trước khi tự động khóa tài khoản cán bộ', 'SECURITY'),
('account_lockout_duration_min', '30', 'Thời gian tạm khóa tài khoản cán bộ khi vượt ngưỡng sai (phút)', 'SECURITY'),
('telco_otp_brandname', 'HAI_PHONG_GOV', 'Tên định danh Brandname SMS gửi mã xác thực người dân', 'TELCO'),
('gateway_heartbeat_interval_sec', '30', 'Khoảng thời gian kiểm tra nhịp tim Gateway OPNsense (giây)', 'GATEWAY');

-- 4. Role Permissions Matrix (Screen C05: Ma trận phân quyền chi tiết cho 5 vai trò)
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    module_name VARCHAR(50) NOT NULL,
    can_view BOOLEAN NOT NULL DEFAULT TRUE,
    can_create BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    can_approve BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (role_name, module_name)
);

-- Seed Role Permissions Matrix
INSERT INTO role_permissions (role_name, module_name, can_view, can_create, can_edit, can_delete, can_approve)
VALUES 
-- SUPER_ADMIN
('ROLE_SUPER_ADMIN', 'LOCATIONS', TRUE, TRUE, TRUE, TRUE, TRUE),
('ROLE_SUPER_ADMIN', 'DEVICES', TRUE, TRUE, TRUE, TRUE, TRUE),
('ROLE_SUPER_ADMIN', 'CAMPAIGNS', TRUE, TRUE, TRUE, TRUE, TRUE),
('ROLE_SUPER_ADMIN', 'INCIDENTS', TRUE, TRUE, TRUE, TRUE, TRUE),
('ROLE_SUPER_ADMIN', 'USERS', TRUE, TRUE, TRUE, TRUE, TRUE),
('ROLE_SUPER_ADMIN', 'SETTINGS', TRUE, TRUE, TRUE, TRUE, TRUE),
-- SOTTTT_ADMIN
('ROLE_SOTTTT_ADMIN', 'LOCATIONS', TRUE, TRUE, TRUE, FALSE, TRUE),
('ROLE_SOTTTT_ADMIN', 'DEVICES', TRUE, FALSE, FALSE, FALSE, FALSE),
('ROLE_SOTTTT_ADMIN', 'CAMPAIGNS', TRUE, TRUE, TRUE, TRUE, TRUE),
('ROLE_SOTTTT_ADMIN', 'INCIDENTS', TRUE, FALSE, FALSE, FALSE, TRUE),
('ROLE_SOTTTT_ADMIN', 'USERS', TRUE, TRUE, TRUE, FALSE, FALSE),
('ROLE_SOTTTT_ADMIN', 'SETTINGS', TRUE, FALSE, FALSE, FALSE, FALSE),
-- DISTRICT_OFFICER
('ROLE_DISTRICT_OFFICER', 'LOCATIONS', TRUE, FALSE, TRUE, FALSE, FALSE),
('ROLE_DISTRICT_OFFICER', 'DEVICES', TRUE, FALSE, FALSE, FALSE, FALSE),
('ROLE_DISTRICT_OFFICER', 'CAMPAIGNS', TRUE, TRUE, FALSE, FALSE, FALSE),
('ROLE_DISTRICT_OFFICER', 'INCIDENTS', TRUE, TRUE, FALSE, FALSE, FALSE),
('ROLE_DISTRICT_OFFICER', 'USERS', FALSE, FALSE, FALSE, FALSE, FALSE),
('ROLE_DISTRICT_OFFICER', 'SETTINGS', FALSE, FALSE, FALSE, FALSE, FALSE),
-- VNPT_ENGINEER
('ROLE_VNPT_ENGINEER', 'LOCATIONS', TRUE, FALSE, FALSE, FALSE, FALSE),
('ROLE_VNPT_ENGINEER', 'DEVICES', TRUE, TRUE, TRUE, FALSE, FALSE),
('ROLE_VNPT_ENGINEER', 'CAMPAIGNS', FALSE, FALSE, FALSE, FALSE, FALSE),
('ROLE_VNPT_ENGINEER', 'INCIDENTS', TRUE, TRUE, TRUE, FALSE, FALSE),
('ROLE_VNPT_ENGINEER', 'USERS', FALSE, FALSE, FALSE, FALSE, FALSE),
('ROLE_VNPT_ENGINEER', 'SETTINGS', FALSE, FALSE, FALSE, FALSE, FALSE);
