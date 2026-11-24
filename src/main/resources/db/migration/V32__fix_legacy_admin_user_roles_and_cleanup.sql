-- ==============================================================================
-- V31: Đồng bộ dứt điểm mọi tài khoản & quyền kỹ thuật viên cũ sang Viettel
-- ==============================================================================

-- 1. Cập nhật mọi tài khoản admin còn mang role cũ hoặc username cũ sang Viettel
UPDATE admin_users
SET role = 'ROLE_VIETTEL_ENGINEER',
    username = CASE WHEN username LIKE '%vnpt%' THEN REPLACE(username, 'vnpt', 'viettel') ELSE username END,
    email = CASE WHEN email LIKE '%vnpt%' THEN REPLACE(email, 'vnpt', 'viettel') ELSE email END,
    full_name = CASE WHEN full_name LIKE '%VNPT%' THEN REPLACE(full_name, 'VNPT', 'Viettel') ELSE full_name END
WHERE role = 'ROLE_VNPT_ENGINEER' OR role LIKE '%VNPT%' OR username LIKE '%vnpt%';

-- 2. Đảm bảo toàn bộ bảng role_permissions dùng ROLE_VIETTEL_ENGINEER
UPDATE role_permissions
SET role_name = 'ROLE_VIETTEL_ENGINEER'
WHERE role_name = 'ROLE_VNPT_ENGINEER' OR role_name LIKE '%VNPT%';

-- 3. Xóa các bản ghi trùng lặp trong role_permissions nếu có sau khi gộp
DELETE FROM role_permissions a USING role_permissions b
WHERE a.id > b.id AND a.role_name = b.role_name AND a.module_name = b.module_name;
