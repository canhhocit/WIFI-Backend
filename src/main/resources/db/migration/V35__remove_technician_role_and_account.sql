-- V35: Bỏ vai trò/tài khoản Kỹ thuật viên theo mô hình 3 cấp (Admin, Sở KH&CN, UBND Xã/Phường)

DELETE FROM admin_users WHERE username = 'viettel_tech01' OR role = 'ROLE_VIETTEL_ENGINEER';

DELETE FROM role_permissions WHERE role_name IN ('ROLE_VIETTEL_ENGINEER', 'ROLE_DISTRICT_OFFICER');
