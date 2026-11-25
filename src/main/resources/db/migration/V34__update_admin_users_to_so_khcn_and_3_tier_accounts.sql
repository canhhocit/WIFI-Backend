-- ==============================================================================
-- V34: Đồng bộ tên cơ quan Sở KH&CN và phân cấp tài khoản 3 cấp (Admin, Sở KH&CN, UBND Xã/Phường)
-- ==============================================================================

-- 1. Cập nhật tài khoản Sở TT&TT sang Sở KH&CN Hải Phòng
UPDATE admin_users
SET full_name = REPLACE(full_name, 'Sở TT&TT', 'Sở KH&CN'),
    updated_at = CURRENT_TIMESTAMP
WHERE full_name LIKE '%Sở TT&TT%';

-- 2. Cập nhật tài khoản Cán bộ UBND Quận Hồng Bàng sang Cán bộ UBND Phường Hồng Bàng (ROLE_COMMUNE_OFFICER)
UPDATE admin_users
SET full_name = 'Cán bộ UBND Phường Hồng Bàng',
    role = 'ROLE_COMMUNE_OFFICER',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'hongbang_officer' OR full_name LIKE '%UBND Quận Hồng Bàng%';

-- 3. Chuyển các tài khoản ROLE_DISTRICT_OFFICER cũ sang ROLE_COMMUNE_OFFICER theo mô hình 3 cấp tài khoản
UPDATE admin_users
SET role = 'ROLE_COMMUNE_OFFICER',
    full_name = REPLACE(REPLACE(full_name, 'UBND Quận', 'UBND Phường'), 'UBND Huyện', 'UBND Xã'),
    updated_at = CURRENT_TIMESTAMP
WHERE role = 'ROLE_DISTRICT_OFFICER';

-- 4. Cập nhật tên cơ quan nhận báo cáo trong lịch sử báo cáo hành chính sang Sở KH&CN
UPDATE government_report_history
SET recipient_agency = REPLACE(recipient_agency, 'Sở TT&TT', 'Sở KH&CN')
WHERE recipient_agency LIKE '%Sở TT&TT%';
