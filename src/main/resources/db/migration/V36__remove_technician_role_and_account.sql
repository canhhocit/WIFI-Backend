-- V36: Bỏ vai trò/tài khoản Kỹ thuật viên theo mô hình 3 cấp (Admin, Sở KH&CN, UBND Xã/Phường)
-- Đồng thời chuẩn hóa và khởi tạo bảng role_permissions cho Cán bộ UBND Xã / Phường (ROLE_COMMUNE_OFFICER)

DELETE FROM admin_users WHERE username = 'viettel_tech01' OR role = 'ROLE_VIETTEL_ENGINEER';

-- 1. Chuyển quyền từ ROLE_DISTRICT_OFFICER sang ROLE_COMMUNE_OFFICER nếu có
UPDATE role_permissions 
SET role_name = 'ROLE_COMMUNE_OFFICER' 
WHERE role_name = 'ROLE_DISTRICT_OFFICER'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp2 
    WHERE rp2.role_name = 'ROLE_COMMUNE_OFFICER' AND rp2.module_name = role_permissions.module_name
  );

-- 2. Xóa quyền của vai trò kỹ thuật viên và vai trò cấp huyện cũ
DELETE FROM role_permissions WHERE role_name IN ('ROLE_VIETTEL_ENGINEER', 'ROLE_DISTRICT_OFFICER');

-- 3. Khởi tạo quyền chuẩn cho Cán bộ UBND Xã / Phường (ROLE_COMMUNE_OFFICER) nếu chưa có
INSERT INTO role_permissions (role_name, module_name, can_view, can_create, can_edit, can_delete, can_approve)
VALUES
('ROLE_COMMUNE_OFFICER', 'LOCATIONS', TRUE, FALSE, TRUE, FALSE, FALSE),
('ROLE_COMMUNE_OFFICER', 'DEVICES', TRUE, FALSE, FALSE, FALSE, FALSE),
('ROLE_COMMUNE_OFFICER', 'CAMPAIGNS', TRUE, TRUE, FALSE, FALSE, FALSE),
('ROLE_COMMUNE_OFFICER', 'INCIDENTS', TRUE, TRUE, FALSE, FALSE, FALSE),
('ROLE_COMMUNE_OFFICER', 'USERS', FALSE, FALSE, FALSE, FALSE, FALSE),
('ROLE_COMMUNE_OFFICER', 'SETTINGS', FALSE, FALSE, FALSE, FALSE, FALSE)
ON CONFLICT (role_name, module_name) DO NOTHING;

-- 4. Đảm bảo quyền chuẩn cho Cán bộ Sở KH&CN (ROLE_SOTTTT_ADMIN) nếu chưa có
INSERT INTO role_permissions (role_name, module_name, can_view, can_create, can_edit, can_delete, can_approve)
VALUES
('ROLE_SOTTTT_ADMIN', 'LOCATIONS', TRUE, TRUE, TRUE, FALSE, TRUE),
('ROLE_SOTTTT_ADMIN', 'DEVICES', TRUE, FALSE, FALSE, FALSE, FALSE),
('ROLE_SOTTTT_ADMIN', 'CAMPAIGNS', TRUE, TRUE, TRUE, TRUE, TRUE),
('ROLE_SOTTTT_ADMIN', 'INCIDENTS', TRUE, FALSE, FALSE, FALSE, TRUE),
('ROLE_SOTTTT_ADMIN', 'USERS', TRUE, TRUE, TRUE, FALSE, FALSE),
('ROLE_SOTTTT_ADMIN', 'SETTINGS', TRUE, FALSE, FALSE, FALSE, FALSE)
ON CONFLICT (role_name, module_name) DO NOTHING;
