-- ==============================================================================
-- V32: Đồng bộ dứt điểm mọi tài khoản & quyền kỹ thuật viên cũ sang Viettel
-- ==============================================================================

-- 1. Nếu đã tồn tại tài khoản viettel_tech01, xóa bản ghi cũ vnpt_tech01 để tránh trùng unique username/email
DELETE FROM admin_users
WHERE (username LIKE '%vnpt%' OR email LIKE '%vnpt%')
  AND EXISTS (SELECT 1 FROM admin_users u2 WHERE u2.username = 'viettel_tech01' OR u2.email = 'kythuat.viettel01@viettel.com.vn');

-- 2. Cập nhật mọi tài khoản còn mang vai trò cũ sang ROLE_VIETTEL_ENGINEER
UPDATE admin_users
SET role = 'ROLE_VIETTEL_ENGINEER'
WHERE role = 'ROLE_VNPT_ENGINEER' OR role LIKE '%VNPT%';

-- 3. Đảm bảo toàn bộ bảng role_permissions dùng ROLE_VIETTEL_ENGINEER
UPDATE role_permissions
SET role_name = 'ROLE_VIETTEL_ENGINEER'
WHERE role_name = 'ROLE_VNPT_ENGINEER' OR role_name LIKE '%VNPT%';

-- 4. Xóa các bản ghi trùng lặp trong role_permissions nếu có sau khi gộp
DELETE FROM role_permissions a USING role_permissions b
WHERE a.id > b.id AND a.role_name = b.role_name AND a.module_name = b.module_name;
