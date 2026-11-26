-- ==============================================================================
-- V38: Chuyển đổi cây địa bàn sang mô hình chính quyền 2 cấp TP. Hải Phòng mới
--      sau sáp nhập theo Nghị quyết 1669/NQ-UBTVQH15 (hiệu lực 01/07/2025):
--      Cấp 1: Thành phố Hải Phòng
--      Cấp 2: 114 đơn vị hành chính cơ sở (45 Phường, 67 Xã, 02 Đặc khu)
--      Đồng thời chuyển toàn bộ điểm phát, tài khoản, chiến dịch và xóa bỏ cấp
--      Quận/Huyện cũ khỏi bảng administrative_units.
-- ==============================================================================

-- 1. Đảm bảo đơn vị Cấp 1 (Thành phố Hải Phòng) tồn tại và đang ACTIVE
INSERT INTO administrative_units (code, name, level, path, status, notes, created_at, updated_at)
SELECT 'HP', 'Thành phố Hải Phòng', 1, '/HP/', 'ACTIVE', 'Thành phố trực thuộc Trung ương (sau hợp nhất Hải Phòng - Hải Dương)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM administrative_units WHERE code = 'HP');

UPDATE administrative_units
SET name = 'Thành phố Hải Phòng',
    level = 1,
    path = '/HP/',
    status = 'ACTIVE',
    parent_id = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'HP';

-- 2. Nạp 45 PHƯỜNG trực thuộc Thành phố Hải Phòng (Cấp 2)
-- Nhóm nội thành & đô thị mở rộng (Hồng Bàng, Ngô Quyền, Lê Chân, Hải An, Kiến An, Đồ Sơn, Dương Kinh, An Dương, Thủy Nguyên)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT hp.id, v.code, v.name, 2, '/HP/' || v.code || '/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units hp
JOIN (
    VALUES
        ('HP_P_HONGBANG', 'Phường Hồng Bàng'),
        ('HP_P_HONGAN', 'Phường Hồng An'),
        ('HP_P_NGOQUYEN', 'Phường Ngô Quyền'),
        ('HP_P_GIAVIEN', 'Phường Gia Viên'),
        ('HP_P_LECHAN', 'Phường Lê Chân'),
        ('HP_P_ANBIEN', 'Phường An Biên'),
        ('HP_P_HAIAN', 'Phường Hải An'),
        ('HP_P_DONGHAI', 'Phường Đông Hải'),
        ('HP_P_KIENAN', 'Phường Kiến An'),
        ('HP_P_PHULIEN', 'Phường Phù Liễn'),
        ('HP_P_DOSON', 'Phường Đồ Sơn'),
        ('HP_P_NAMDOSON', 'Phường Nam Đồ Sơn'),
        ('HP_P_DUONGKINH', 'Phường Dương Kinh'),
        ('HP_P_HUNGDAO', 'Phường Hưng Đạo'),
        ('HP_P_ANDUONG', 'Phường An Dương'),
        ('HP_P_ANHAI', 'Phường An Hải'),
        ('HP_P_ANPHONG', 'Phường An Phong'),
        ('HP_P_THUYNGUYEN', 'Phường Thủy Nguyên'),
        ('HP_P_THIENHUONG', 'Phường Thiên Hương'),
        ('HP_P_HOABINH', 'Phường Hòa Bình'),
        ('HP_P_NAMTRIEU', 'Phường Nam Triệu'),
        ('HP_P_BACHDANG', 'Phường Bạch Đằng'),
        ('HP_P_LUUKIEM', 'Phường Lưu Kiếm'),
        ('HP_P_LEICHMOC', 'Phường Lê Ích Mộc'),
        ('HP_P_HAIDUONG', 'Phường Hải Dương'),
        ('HP_P_LETHANHNGHI', 'Phường Lê Thanh Nghị'),
        ('HP_P_VIETHOA', 'Phường Việt Hòa'),
        ('HP_P_THANHDONG', 'Phường Thành Đông'),
        ('HP_P_NAMDONG', 'Phường Nam Đồng'),
        ('HP_P_TANHUNG', 'Phường Tân Hưng'),
        ('HP_P_THACHKHOI', 'Phường Thạch Khôi'),
        ('HP_P_TUMINH', 'Phường Tứ Minh'),
        ('HP_P_AIQUOC', 'Phường Ái Quốc'),
        ('HP_P_CHUVANAN', 'Phường Chu Văn An'),
        ('HP_P_CHILINH', 'Phường Chí Linh'),
        ('HP_P_TRANHUNGDAO', 'Phường Trần Hưng Đạo'),
        ('HP_P_TRANNHANTONG', 'Phường Trần Nhân Tông'),
        ('HP_P_NGUYENTRAI', 'Phường Nguyễn Trãi'),
        ('HP_P_LEDAIHANH', 'Phường Lê Đại Hành'),
        ('HP_P_KINHMON', 'Phường Kinh Môn'),
        ('HP_P_NGUYENDAINANG', 'Phường Nguyễn Đại Năng'),
        ('HP_P_TRANLIEU', 'Phường Trần Liễu'),
        ('HP_P_BACANPHU', 'Phường Bắc An Phụ'),
        ('HP_P_PHAMSUMANH', 'Phường Phạm Sư Mạnh'),
        ('HP_P_NHICHIEU', 'Phường Nhị Chiểu')
) AS v(code, name) ON 1 = 1
WHERE hp.code = 'HP'
  AND NOT EXISTS (SELECT 1 FROM administrative_units u WHERE u.code = v.code);

-- 3. Nạp 67 XÃ trực thuộc Thành phố Hải Phòng (Cấp 2)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT hp.id, v.code, v.name, 2, '/HP/' || v.code || '/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units hp
JOIN (
    VALUES
        ('HP_X_ANHUNG', 'Xã An Hưng'),
        ('HP_X_ANKHANH', 'Xã An Khánh'),
        ('HP_X_ANQUANG', 'Xã An Quang'),
        ('HP_X_ANTRUONG', 'Xã An Trường'),
        ('HP_X_ANLAO', 'Xã An Lão'),
        ('HP_X_KIENTHUY', 'Xã Kiến Thụy'),
        ('HP_X_KIENMINH', 'Xã Kiến Minh'),
        ('HP_X_KIENHAI', 'Xã Kiến Hải'),
        ('HP_X_KIENHUNG', 'Xã Kiến Hưng'),
        ('HP_X_NGHIDUONG', 'Xã Nghi Dương'),
        ('HP_X_QUYETTHANG', 'Xã Quyết Thắng'),
        ('HP_X_TIENLANG', 'Xã Tiên Lãng'),
        ('HP_X_TANMINH', 'Xã Tân Minh'),
        ('HP_X_TIENMINH', 'Xã Tiên Minh'),
        ('HP_X_CHANHUNG', 'Xã Chấn Hưng'),
        ('HP_X_HUNGTHANG', 'Xã Hùng Thắng'),
        ('HP_X_VINHBAO', 'Xã Vĩnh Bảo'),
        ('HP_X_NGUYENBINHKHIEM', 'Xã Nguyễn Bỉnh Khiêm'),
        ('HP_X_VINHAM', 'Xã Vĩnh Am'),
        ('HP_X_VINHHAI', 'Xã Vĩnh Hải'),
        ('HP_X_VINHHOA', 'Xã Vĩnh Hòa'),
        ('HP_X_VINHTHINH', 'Xã Vĩnh Thịnh'),
        ('HP_X_VINHTHUAN', 'Xã Vĩnh Thuận'),
        ('HP_X_VIETKHE', 'Xã Việt Khê'),
        ('HP_X_NAMANPHU', 'Xã Nam An Phụ'),
        ('HP_X_NAMSACH', 'Xã Nam Sách'),
        ('HP_X_THAITAN', 'Xã Thái Tân'),
        ('HP_X_TRANPHU', 'Xã Trần Phú'),
        ('HP_X_HOPTIEN', 'Xã Hợp Tiến'),
        ('HP_X_ANPHU', 'Xã An Phú'),
        ('HP_X_THANHHA', 'Xã Thanh Hà'),
        ('HP_X_HATAY', 'Xã Hà Tây'),
        ('HP_X_HABAC', 'Xã Hà Bắc'),
        ('HP_X_HANAM', 'Xã Hà Nam'),
        ('HP_X_HADONG', 'Xã Hà Đông'),
        ('HP_X_MAODIEN', 'Xã Mao Điền'),
        ('HP_X_CAMGIANG', 'Xã Cẩm Giàng'),
        ('HP_X_CAM_GIANG', 'Xã Cẩm Giang'),
        ('HP_X_TUETINH', 'Xã Tuệ Tĩnh'),
        ('HP_X_KESAT', 'Xã Kẻ Sặt'),
        ('HP_X_BINHGIANG', 'Xã Bình Giang'),
        ('HP_X_DUONGAN', 'Xã Đường An'),
        ('HP_X_THUONGHONG', 'Xã Thượng Hồng'),
        ('HP_X_GIALOC', 'Xã Gia Lộc'),
        ('HP_X_YETKIEU', 'Xã Yết Kiêu'),
        ('HP_X_GIAPHUC', 'Xã Gia Phúc'),
        ('HP_X_TRUONGTAN', 'Xã Trường Tân'),
        ('HP_X_TUKY', 'Xã Tứ Kỳ'),
        ('HP_X_TANKY', 'Xã Tân Kỳ'),
        ('HP_X_DAISON', 'Xã Đại Sơn'),
        ('HP_X_CHIMINH', 'Xã Chí Minh'),
        ('HP_X_LACPHUONG', 'Xã Lạc Phượng'),
        ('HP_X_NGUYENGIAP', 'Xã Nguyên Giáp'),
        ('HP_X_NINHGIANG', 'Xã Ninh Giang'),
        ('HP_X_VINHLAI', 'Xã Vĩnh Lại'),
        ('HP_X_KHUCTHUADU', 'Xã Khúc Thừa Dụ'),
        ('HP_X_TANAN', 'Xã Tân An'),
        ('HP_X_HONGCHAU', 'Xã Hồng Châu'),
        ('HP_X_THANHMIEN', 'Xã Thanh Miện'),
        ('HP_X_BACTHANHMIEN', 'Xã Bắc Thanh Miện'),
        ('HP_X_HAIHUNG', 'Xã Hải Hưng'),
        ('HP_X_NGUYENLUONGBANG', 'Xã Nguyễn Lương Bằng'),
        ('HP_X_NAMTHANHMIEN', 'Xã Nam Thanh Miện'),
        ('HP_X_PHUTHAI', 'Xã Phú Thái'),
        ('HP_X_LAIKHE', 'Xã Lai Khê'),
        ('HP_X_ANTHANH', 'Xã An Thành'),
        ('HP_X_KIMTHANH', 'Xã Kim Thành')
) AS v(code, name) ON 1 = 1
WHERE hp.code = 'HP'
  AND NOT EXISTS (SELECT 1 FROM administrative_units u WHERE u.code = v.code);

-- 4. Nạp 02 ĐẶC KHU trực thuộc Thành phố Hải Phòng (Cấp 2)
INSERT INTO administrative_units (parent_id, code, name, level, path, status, created_at, updated_at)
SELECT hp.id, v.code, v.name, 2, '/HP/' || v.code || '/', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM administrative_units hp
JOIN (
    VALUES
        ('HP_DZ_CATHAI', 'Đặc khu Cát Hải'),
        ('HP_DZ_BACHLONGVI', 'Đặc khu Bạch Long Vĩ')
) AS v(code, name) ON 1 = 1
WHERE hp.code = 'HP'
  AND NOT EXISTS (SELECT 1 FROM administrative_units u WHERE u.code = v.code);

-- 5. Chuyển đổi dữ liệu wifi_locations đang gắn với Quận/Huyện hoặc Phường/Xã cũ sang 114 đơn vị mới
UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_HONGBANG' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_HB', 'HP_HB_HVTHU', 'HP_HB_THUONGLY', 'HP_HB_SODAU'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_NGOQUYEN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_NQ', 'HP_NQ_MAYTO'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_GIAVIEN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_NQ_LACHTRAY', 'HP_NQ_CAUDAT'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_ANBIEN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_LC_ANBIEN', 'HP_LC_TRANNGUYENHAN'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_LECHAN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_LC', 'HP_LC_KENHDUONG'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_HAIAN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_HA', 'HP_HA_DANGLAM'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_DONGHAI' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_HA_DONGHAI1'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_KIENAN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_KA', 'HP_KA_BACSON'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_PHULIEN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_KA_NGOCSON'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_DOSON' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_DS', 'HP_DS_HAISON', 'HP_DS_VANHUONG'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_HUNGDAO' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_DK_HUNGDAO', 'HP_DK_ANHDUNG'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_DUONGKINH' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_DK'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_THUYNGUYEN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_TN', 'HP_TN_NUITHANH', 'HP_TN_THUYDUONG'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_ANDUONG' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_AD', 'HP_AD_TTANDUONG'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_ANHAI' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_AD_ANDONG'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_X_ANLAO' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_AL', 'HP_AL_TTANLAO', 'HP_AL_TRUONGSON'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_X_KIENTHUY' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_KT', 'HP_KT_NUIDOI', 'HP_KT_TANTRAO'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_X_TIENLANG' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_TL', 'HP_TL_TTTIENLANG', 'HP_TL_QUYETTIEN'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_X_VINHBAO' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_VB', 'HP_VB_TTVINHBAO', 'HP_VB_TAMDA'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_DZ_CATHAI' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_CH', 'HP_CH_CATBA', 'HP_CH_VIETHAI'));

UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_DZ_BACHLONGVI' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_BLV', 'HP_BLV_DAO'));

-- Bảo đảm không còn điểm phát nào trỏ vào đơn vị cũ trước khi xóa
UPDATE wifi_locations
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_HONGBANG' LIMIT 1)
WHERE admin_unit_id IS NOT NULL
  AND admin_unit_id NOT IN (
      SELECT id FROM administrative_units
      WHERE code = 'HP' OR code LIKE 'HP_P_%' OR code LIKE 'HP_X_%' OR code LIKE 'HP_DZ_%'
  );

-- Đồng bộ nhãn địa chỉ mẫu ở wifi_locations cho khớp sau sáp nhập
UPDATE wifi_locations
SET address = 'Số 28 Đinh Tiên Hoàng, Phường Hồng Bàng, TP Hải Phòng',
    name = 'Nhà văn hóa TDP số 1 - Phường Hồng Bàng',
    welcome_message = 'Chào mừng Nhân dân đến với Nhà văn hóa TDP số 1 - Phường Hồng Bàng',
    promo_title = 'Chuyển đổi số cộng đồng - Phường Hồng Bàng'
WHERE slug = 'hp-hb-nvh01';

UPDATE wifi_locations
SET address = 'Số 15 Lê Lai, Phường Ngô Quyền, TP Hải Phòng',
    welcome_message = 'Chào mừng Nhân dân đến với Nhà văn hóa Khu dân cư Máy Tơ - Phường Ngô Quyền'
WHERE slug = 'hp-nq-nvh02';

UPDATE wifi_locations
SET address = 'Khu 3, Đặc khu Cát Hải, TP Hải Phòng',
    name = 'Nhà văn hóa Đặc khu Cát Hải',
    welcome_message = 'Chào mừng Nhân dân và Du khách đến với Nhà văn hóa Đặc khu Cát Hải'
WHERE slug = 'hp-ch-nvh03';

UPDATE wifi_locations
SET address = 'Khu phố Bạch Đằng, Phường Thủy Nguyên, TP Hải Phòng',
    name = 'Nhà văn hóa Trung tâm Phường Thủy Nguyên',
    welcome_message = 'Chào mừng Nhân dân đến với Nhà văn hóa Trung tâm Phường Thủy Nguyên'
WHERE slug = 'hp-tn-nvh04';

-- 6. Chuyển đổi tài khoản cán bộ (admin_users) đang gắn với Quận/Huyện hoặc Phường cũ sang đơn vị mới
UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_HONGBANG' LIMIT 1),
    full_name = 'Cán bộ UBND Phường Hồng Bàng',
    role = 'ROLE_COMMUNE_OFFICER'
WHERE username = 'hongbang_officer';

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_HONGBANG' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_HB', 'HP_HB_HVTHU', 'HP_HB_THUONGLY', 'HP_HB_SODAU'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_NGOQUYEN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_NQ', 'HP_NQ_MAYTO', 'HP_NQ_LACHTRAY', 'HP_NQ_CAUDAT'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_LECHAN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_LC', 'HP_LC_ANBIEN', 'HP_LC_TRANNGUYENHAN', 'HP_LC_KENHDUONG'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_HAIAN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_HA', 'HP_HA_DANGLAM', 'HP_HA_DONGHAI1'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_KIENAN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_KA', 'HP_KA_BACSON', 'HP_KA_NGOCSON'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_DOSON' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_DS', 'HP_DS_HAISON', 'HP_DS_VANHUONG'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_DUONGKINH' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_DK', 'HP_DK_ANHDUNG', 'HP_DK_HUNGDAO'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_THUYNGUYEN' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_TN', 'HP_TN_NUITHANH', 'HP_TN_THUYDUONG'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_ANDUONG' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_AD', 'HP_AD_TTANDUONG', 'HP_AD_ANDONG'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_X_ANLAO' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_AL', 'HP_AL_TTANLAO', 'HP_AL_TRUONGSON'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_X_KIENTHUY' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_KT', 'HP_KT_NUIDOI', 'HP_KT_TANTRAO'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_X_TIENLANG' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_TL', 'HP_TL_TTTIENLANG', 'HP_TL_QUYETTIEN'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_X_VINHBAO' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_VB', 'HP_VB_TTVINHBAO', 'HP_VB_TAMDA'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_DZ_CATHAI' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_CH', 'HP_CH_CATBA', 'HP_CH_VIETHAI'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_DZ_BACHLONGVI' LIMIT 1)
WHERE admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_BLV', 'HP_BLV_DAO'));

UPDATE admin_users
SET admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_HONGBANG' LIMIT 1)
WHERE admin_unit_id IS NOT NULL
  AND admin_unit_id NOT IN (
      SELECT id FROM administrative_units
      WHERE code = 'HP' OR code LIKE 'HP_P_%' OR code LIKE 'HP_X_%' OR code LIKE 'HP_DZ_%'
  );

-- 7. Chuyển đổi chiến dịch (campaigns) đang gắn với Quận/Huyện cũ sang đơn vị mới (hoặc NULL phát toàn TP nếu không khớp)
UPDATE campaigns
SET target_admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_HONGBANG' LIMIT 1)
WHERE target_admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_HB', 'HP_HB_HVTHU', 'HP_HB_THUONGLY', 'HP_HB_SODAU'));

UPDATE campaigns
SET target_admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_NGOQUYEN' LIMIT 1)
WHERE target_admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_NQ', 'HP_NQ_MAYTO', 'HP_NQ_LACHTRAY', 'HP_NQ_CAUDAT'));

UPDATE campaigns
SET target_admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_LECHAN' LIMIT 1)
WHERE target_admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_LC', 'HP_LC_ANBIEN', 'HP_LC_TRANNGUYENHAN', 'HP_LC_KENHDUONG'));

UPDATE campaigns
SET target_admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_HAIAN' LIMIT 1)
WHERE target_admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_HA', 'HP_HA_DANGLAM', 'HP_HA_DONGHAI1'));

UPDATE campaigns
SET target_admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_KIENAN' LIMIT 1)
WHERE target_admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_KA', 'HP_KA_BACSON', 'HP_KA_NGOCSON'));

UPDATE campaigns
SET target_admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_DOSON' LIMIT 1)
WHERE target_admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_DS', 'HP_DS_HAISON', 'HP_DS_VANHUONG'));

UPDATE campaigns
SET target_admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_DUONGKINH' LIMIT 1)
WHERE target_admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_DK', 'HP_DK_ANHDUNG', 'HP_DK_HUNGDAO'));

UPDATE campaigns
SET target_admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_THUYNGUYEN' LIMIT 1)
WHERE target_admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_TN', 'HP_TN_NUITHANH', 'HP_TN_THUYDUONG'));

UPDATE campaigns
SET target_admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_P_ANDUONG' LIMIT 1)
WHERE target_admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_AD', 'HP_AD_TTANDUONG', 'HP_AD_ANDONG'));

UPDATE campaigns
SET target_admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_DZ_CATHAI' LIMIT 1)
WHERE target_admin_unit_id IN (SELECT id FROM administrative_units WHERE code IN ('HP_CH', 'HP_CH_CATBA', 'HP_CH_VIETHAI'));

UPDATE campaigns
SET target_admin_unit_id = NULL
WHERE target_admin_unit_id IS NOT NULL
  AND target_admin_unit_id NOT IN (
      SELECT id FROM administrative_units
      WHERE code = 'HP' OR code LIKE 'HP_P_%' OR code LIKE 'HP_X_%' OR code LIKE 'HP_DZ_%'
  );

-- 8. Gỡ tự tham chiếu parent_id/merged_to_id và XÓA HẲN toàn bộ Quận/Huyện & Phường/Xã cũ
UPDATE administrative_units
SET parent_id = NULL,
    merged_to_id = NULL
WHERE code <> 'HP'
  AND code NOT LIKE 'HP_P_%'
  AND code NOT LIKE 'HP_X_%'
  AND code NOT LIKE 'HP_DZ_%';

UPDATE administrative_units
SET merged_to_id = NULL
WHERE merged_to_id IN (
    SELECT id FROM administrative_units
    WHERE code <> 'HP'
      AND code NOT LIKE 'HP_P_%'
      AND code NOT LIKE 'HP_X_%'
      AND code NOT LIKE 'HP_DZ_%'
);

DELETE FROM administrative_units
WHERE code <> 'HP'
  AND code NOT LIKE 'HP_P_%'
  AND code NOT LIKE 'HP_X_%'
  AND code NOT LIKE 'HP_DZ_%';
