-- ==============================================================================
-- V28: Đồng bộ toàn bộ tài khoản kỹ thuật viên, phân quyền và ISP sang Viettel
-- ==============================================================================

-- 1. Cập nhật tài khoản kỹ thuật viên
UPDATE admin_users
SET username = 'viettel_tech01',
    email = 'kythuat.viettel01@viettel.com.vn',
    full_name = 'Kỹ thuật viên Viễn thông Viettel',
    role = 'ROLE_VIETTEL_ENGINEER'
WHERE username LIKE '%tech01%' OR role LIKE '%ENGINEER%';

-- 2. Cập nhật bảng phân quyền vai trò
UPDATE role_permissions
SET role = 'ROLE_VIETTEL_ENGINEER'
WHERE role LIKE '%ENGINEER%';

-- 3. Cập nhật phân công sự cố
UPDATE incident_tickets
SET assigned_to = 'Viettel_KyThuat_01'
WHERE assigned_to LIKE '%tech%' OR assigned_to LIKE '%Engineer%' OR assigned_to LIKE '%KyThuat%';

-- 4. Đảm bảo toàn bộ địa điểm dùng nhà mạng Viettel
UPDATE wifi_locations
SET isp_name = 'Viettel'
WHERE isp_name IS NULL OR isp_name != 'Viettel';
