-- V42: Cập nhật và sửa lại ma trận phân quyền chuẩn cho Cán bộ UBND Xã / Phường (ROLE_COMMUNE_OFFICER)
-- và Cán bộ Sở KH&CN Hải Phòng (ROLE_SOTTTT_ADMIN) theo mô hình quản lý chính quyền 3 cấp

DELETE FROM role_permissions WHERE role_name IN ('ROLE_COMMUNE_OFFICER', 'ROLE_DISTRICT_OFFICER');

INSERT INTO role_permissions (role_name, module_name, can_view, can_create, can_edit, can_delete, can_approve)
VALUES
('ROLE_COMMUNE_OFFICER', 'LOCATIONS', TRUE, FALSE, TRUE, FALSE, FALSE),
('ROLE_COMMUNE_OFFICER', 'DEVICES', TRUE, FALSE, FALSE, FALSE, FALSE),
('ROLE_COMMUNE_OFFICER', 'CAMPAIGNS', TRUE, TRUE, FALSE, FALSE, FALSE),
('ROLE_COMMUNE_OFFICER', 'INCIDENTS', TRUE, TRUE, FALSE, FALSE, FALSE),
('ROLE_COMMUNE_OFFICER', 'USERS', FALSE, FALSE, FALSE, FALSE, FALSE),
('ROLE_COMMUNE_OFFICER', 'SETTINGS', FALSE, FALSE, FALSE, FALSE, FALSE);

-- Đảm bảo quyền chuẩn cho Sở KH&CN (ROLE_SOTTTT_ADMIN)
INSERT INTO role_permissions (role_name, module_name, can_view, can_create, can_edit, can_delete, can_approve)
VALUES
('ROLE_SOTTTT_ADMIN', 'LOCATIONS', TRUE, TRUE, TRUE, FALSE, TRUE),
('ROLE_SOTTTT_ADMIN', 'DEVICES', TRUE, FALSE, FALSE, FALSE, FALSE),
('ROLE_SOTTTT_ADMIN', 'CAMPAIGNS', TRUE, TRUE, TRUE, TRUE, TRUE),
('ROLE_SOTTTT_ADMIN', 'INCIDENTS', TRUE, FALSE, FALSE, FALSE, TRUE),
('ROLE_SOTTTT_ADMIN', 'USERS', TRUE, TRUE, TRUE, FALSE, FALSE),
('ROLE_SOTTTT_ADMIN', 'SETTINGS', TRUE, FALSE, FALSE, FALSE, FALSE)
ON CONFLICT (role_name, module_name) DO UPDATE SET
    can_view = EXCLUDED.can_view,
    can_create = EXCLUDED.can_create,
    can_edit = EXCLUDED.can_edit,
    can_delete = EXCLUDED.can_delete,
    can_approve = EXCLUDED.can_approve;
