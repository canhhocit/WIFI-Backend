-- ==============================================================================
-- V33: Hoàn thiện cây đơn vị hành chính 3 cấp (Thành phố -> Quận/Huyện -> Phường/Xã)
--      và gán địa bàn chuẩn cho tài khoản cán bộ mẫu
-- ==============================================================================

-- 1. Bổ sung các Phường/Xã/Thị trấn (Cấp 3) đại diện cho đầy đủ 15 Quận/Huyện của TP Hải Phòng
-- Quận Hồng Bàng (HP_HB)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_HB_THUONGLY', 'Phường Thượng Lý', 3, '/HP/HP_HB/HP_HB_THUONGLY/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_HB'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_HB_THUONGLY');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_HB_SODAU', 'Phường Sở Dầu', 3, '/HP/HP_HB/HP_HB_SODAU/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_HB'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_HB_SODAU');

-- Quận Ngô Quyền (HP_NQ)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_NQ_LACHTRAY', 'Phường Lạch Tray', 3, '/HP/HP_NQ/HP_NQ_LACHTRAY/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_NQ'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_NQ_LACHTRAY');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_NQ_CAUDAT', 'Phường Cầu Đất', 3, '/HP/HP_NQ/HP_NQ_CAUDAT/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_NQ'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_NQ_CAUDAT');

-- Quận Lê Chân (HP_LC)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_LC_TRANNGUYENHAN', 'Phường Trần Nguyên Hãn', 3, '/HP/HP_LC/HP_LC_TRANNGUYENHAN/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_LC'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_LC_TRANNGUYENHAN');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_LC_KENHDUONG', 'Phường Kênh Dương', 3, '/HP/HP_LC/HP_LC_KENHDUONG/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_LC'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_LC_KENHDUONG');

-- Quận Hải An (HP_HA)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_HA_DANGLAM', 'Phường Đằng Lâm', 3, '/HP/HP_HA/HP_HA_DANGLAM/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_HA'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_HA_DANGLAM');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_HA_DONGHAI1', 'Phường Đông Hải 1', 3, '/HP/HP_HA/HP_HA_DONGHAI1/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_HA'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_HA_DONGHAI1');

-- Quận Kiến An (HP_KA)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_KA_BACSON', 'Phường Bắc Sơn', 3, '/HP/HP_KA/HP_KA_BACSON/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_KA'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_KA_BACSON');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_KA_NGOCSON', 'Phường Ngọc Sơn', 3, '/HP/HP_KA/HP_KA_NGOCSON/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_KA'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_KA_NGOCSON');

-- Quận Đồ Sơn (HP_DS)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_DS_HAISON', 'Phường Hải Sơn', 3, '/HP/HP_DS/HP_DS_HAISON/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_DS'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_DS_HAISON');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_DS_VANHUONG', 'Phường Vạn Hương', 3, '/HP/HP_DS/HP_DS_VANHUONG/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_DS'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_DS_VANHUONG');

-- Quận Dương Kinh (HP_DK)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_DK_ANHDUNG', 'Phường Anh Dũng', 3, '/HP/HP_DK/HP_DK_ANHDUNG/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_DK'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_DK_ANHDUNG');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_DK_HUNGDAO', 'Phường Hưng Đạo', 3, '/HP/HP_DK/HP_DK_HUNGDAO/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_DK'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_DK_HUNGDAO');

-- Huyện Thủy Nguyên (HP_TN)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_TN_THUYDUONG', 'Xã Thủy Đường', 3, '/HP/HP_TN/HP_TN_THUYDUONG/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_TN'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_TN_THUYDUONG');

-- Huyện An Dương (HP_AD)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_AD_TTANDUONG', 'Thị trấn An Dương', 3, '/HP/HP_AD/HP_AD_TTANDUONG/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_AD'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_AD_TTANDUONG');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_AD_ANDONG', 'Xã An Đồng', 3, '/HP/HP_AD/HP_AD_ANDONG/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_AD'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_AD_ANDONG');

-- Huyện An Lão (HP_AL)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_AL_TTANLAO', 'Thị trấn An Lão', 3, '/HP/HP_AL/HP_AL_TTANLAO/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_AL'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_AL_TTANLAO');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_AL_TRUONGSON', 'Thị trấn Trường Sơn', 3, '/HP/HP_AL/HP_AL_TRUONGSON/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_AL'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_AL_TRUONGSON');

-- Huyện Kiến Thụy (HP_KT)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_KT_NUIDOI', 'Thị trấn Núi Đối', 3, '/HP/HP_KT/HP_KT_NUIDOI/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_KT'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_KT_NUIDOI');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_KT_TANTRAO', 'Xã Tân Trào', 3, '/HP/HP_KT/HP_KT_TANTRAO/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_KT'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_KT_TANTRAO');

-- Huyện Tiên Lãng (HP_TL)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_TL_TTTIENLANG', 'Thị trấn Tiên Lãng', 3, '/HP/HP_TL/HP_TL_TTTIENLANG/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_TL'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_TL_TTTIENLANG');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_TL_QUYETTIEN', 'Xã Quyết Tiến', 3, '/HP/HP_TL/HP_TL_QUYETTIEN/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_TL'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_TL_QUYETTIEN');

-- Huyện Vĩnh Bảo (HP_VB)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_VB_TTVINHBAO', 'Thị trấn Vĩnh Bảo', 3, '/HP/HP_VB/HP_VB_TTVINHBAO/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_VB'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_VB_TTVINHBAO');

INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_VB_TAMDA', 'Xã Tam Đa', 3, '/HP/HP_VB/HP_VB_TAMDA/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_VB'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_VB_TAMDA');

-- Huyện Cát Hải (HP_CH)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_CH_VIETHAI', 'Xã Việt Hải', 3, '/HP/HP_CH/HP_CH_VIETHAI/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_CH'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_CH_VIETHAI');

-- Huyện Bạch Long Vĩ (HP_BLV)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT id, 'HP_BLV_DAO', 'Khu dân cư Đảo Bạch Long Vĩ', 3, '/HP/HP_BLV/HP_BLV_DAO/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_BLV'
AND NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP_BLV_DAO');

-- 2. Gán đúng địa bàn Quận Hồng Bàng (Cấp 2) cho tài khoản mẫu hongbang_officer nếu đang trống hoặc gán nhầm Cấp 1
UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_HB' LIMIT 1)
WHERE username = 'hongbang_officer'
  AND (admin_unit_id IS NULL OR admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP' LIMIT 1));
