-- ==============================================================================
-- V10: Thay thế toàn bộ dữ liệu mẫu cũ (Tân Sơn Nhất, Circle K) bằng các Nhà văn hóa Hải Phòng
-- ==============================================================================

-- 1. Cập nhật điểm Hồng Bàng (thay thế circle-k-central nếu còn)
UPDATE wifi_locations
SET slug = 'hp-hb-nvh01',
    location_code = 'LOC-HP-HB-001',
    name = 'Nhà văn hóa TDP số 1 - Phường Hoàng Văn Thụ',
    address = 'Số 28 Đinh Tiên Hoàng, Phường Hoàng Văn Thụ, Quận Hồng Bàng, TP Hải Phòng',
    description = 'Điểm phát sóng Wi-Fi công cộng miễn phí phục vụ hội họp dân phố và dịch vụ công trực tuyến.',
    session_duration_minutes = 120,
    max_bandwidth_mbps = 20,
    welcome_message = 'Chào mừng Nhân dân đến với Nhà văn hóa TDP số 1 - Phường Hoàng Văn Thụ',
    terms_and_conditions = 'Quy chế sử dụng mạng Wi-Fi công cộng Hải Phòng: Hệ thống mạng Wi-Fi công cộng phục vụ miễn phí nhu cầu tra cứu thông tin chính sách, thực hiện dịch vụ công trực tuyến và các tiện ích đời sống văn hóa. Nghiêm cấm truy cập nội dung độc hại, phát tán mã độc hoặc các hành vi vi phạm pháp luật an ninh mạng.',
    promo_title = 'Chuyển đổi số cộng đồng - Phường Hoàng Văn Thụ',
    promo_content = 'Đoàn Thanh niên và Tổ công nghệ số cộng đồng trực hỗ trợ người dân kích hoạt VNeID mức 2 và nộp hồ sơ trực tuyến từ 8h - 17h.',
    promo_banner_url = '/banners/haiphong_culture_house.jpg',
    promo_action_url = 'https://dichvucong.haiphong.gov.vn',
    admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_HB_HVTHU' LIMIT 1),
    latitude = 20.8651,
    longitude = 106.6838,
    status = 'ACTIVE',
    is_active = TRUE,
    isp_name = 'Viettel',
    circuit_code = 'HP-HB-FTTH-8891',
    contact_name = 'Nguyễn Văn Hùng (Tổ trưởng TDP 1)',
    contact_phone = '0912.345.678',
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'circle-k-central' OR slug = 'hp-hb-nvh01';

-- 2. Cập nhật dứt điểm điểm Sân bay Tân Sơn Nhất thành Nhà văn hóa Máy Tơ (Quận Ngô Quyền)
UPDATE wifi_locations
SET slug = 'hp-nq-nvh02',
    location_code = 'LOC-HP-NQ-002',
    name = 'Nhà văn hóa Khu dân cư Máy Tơ',
    address = 'Số 15 Lê Lai, Phường Máy Tơ, Quận Ngô Quyền, TP Hải Phòng',
    description = 'Điểm sinh hoạt cộng đồng và trạm thông tin số dành cho nhân dân phường Máy Tơ.',
    session_duration_minutes = 120,
    max_bandwidth_mbps = 20,
    welcome_message = 'Chào mừng Nhân dân đến với Nhà văn hóa Khu dân cư Máy Tơ - Quận Ngô Quyền',
    terms_and_conditions = 'Quy chế sử dụng mạng Wi-Fi công cộng Hải Phòng: Hệ thống mạng Wi-Fi công cộng phục vụ miễn phí nhu cầu tra cứu thông tin chính sách, thực hiện dịch vụ công trực tuyến và các tiện ích đời sống văn hóa. Nghiêm cấm truy cập nội dung độc hại, phát tán mã độc hoặc các hành vi vi phạm pháp luật an ninh mạng.',
    promo_title = 'Khu dân cư văn hóa điện tử Máy Tơ',
    promo_content = 'Hướng dẫn thanh toán tiền điện, nước không dùng tiền mặt và tra cứu thủ tục đất đai tại Cổng Dịch vụ công Hải Phòng.',
    promo_banner_url = '/banners/central.jpg',
    promo_action_url = 'https://dichvucong.haiphong.gov.vn',
    admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_NQ_MAYTO' LIMIT 1),
    latitude = 20.8624,
    longitude = 106.6912,
    status = 'ACTIVE',
    is_active = TRUE,
    isp_name = 'Viettel Hải Phòng',
    circuit_code = 'HP-NQ-METRO-4421',
    contact_name = 'Trần Thị Mai (Trưởng Ban công tác Mặt trận)',
    contact_phone = '0983.123.456',
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'circle-k-airport' OR slug = 'hp-nq-nvh02';

-- 3. Cập nhật điểm Bờ sông thành Nhà văn hóa Đảo Cát Bà (Huyện Cát Hải)
UPDATE wifi_locations
SET slug = 'hp-ch-nvh03',
    location_code = 'LOC-HP-CH-003',
    name = 'Nhà văn hóa Thị trấn Cát Bà',
    address = 'Khu 3, Thị trấn Cát Bà, Huyện Cát Hải, TP Hải Phòng',
    description = 'Trạm Wi-Fi công cộng phục vụ bà con nhân dân đảo Cát Bà và du khách tra cứu thông tin du lịch và dịch vụ công.',
    session_duration_minutes = 120,
    max_bandwidth_mbps = 25,
    welcome_message = 'Chào mừng Nhân dân và Du khách đến với Nhà văn hóa Thị trấn Cát Bà',
    terms_and_conditions = 'Quy chế sử dụng mạng Wi-Fi công cộng Hải Phòng: Mạng Wi-Fi công cộng phục vụ miễn phí nhu cầu tra cứu thông tin chính sách, du lịch sinh thái Cát Bà, thực hiện dịch vụ công trực tuyến.',
    promo_title = 'Du lịch thông minh & Dịch vụ công Cát Bà',
    promo_content = 'Quét mã QR tra cứu lịch tàu phà bến Đồng Bài, cẩm nang du lịch xanh và cổng dịch vụ công trực tuyến huyện Cát Hải.',
    promo_banner_url = '/banners/riverside.jpg',
    promo_action_url = 'https://dichvucong.haiphong.gov.vn',
    admin_unit_id = (SELECT id FROM administrative_units WHERE code = 'HP_CH_CATBA' LIMIT 1),
    latitude = 20.7275,
    longitude = 107.0494,
    status = 'ACTIVE',
    is_active = TRUE,
    isp_name = 'Viettel',
    circuit_code = 'HP-CH-FO-1102',
    contact_name = 'Lê Tuấn Anh (Cán bộ VHTT TT Cát Bà)',
    contact_phone = '0904.567.890',
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'circle-k-riverside' OR slug = 'hp-ch-nvh03';

-- 4. Bổ sung điểm Nhà văn hóa Núi Đèo (Huyện Thủy Nguyên) nếu chưa tồn tại
INSERT INTO wifi_locations (
    slug, location_code, name, address, description,
    session_duration_minutes, max_bandwidth_mbps,
    terms_and_conditions, welcome_message,
    promo_title, promo_content, promo_banner_url, promo_action_url,
    admin_unit_id, latitude, longitude, status, is_active,
    isp_name, circuit_code, contact_name, contact_phone,
    created_at, updated_at
)
SELECT
    'hp-tn-nvh04',
    'LOC-HP-TN-004',
    'Nhà văn hóa Trung tâm Thị trấn Núi Đèo',
    'Khu phố Bạch Đằng, Thị trấn Núi Đèo, Huyện Thủy Nguyên, TP Hải Phòng',
    'Trung tâm sinh hoạt chính trị - văn hóa cộng đồng hướng tới xây dựng Thành phố Thủy Nguyên.',
    120,
    20,
    'Quy chế sử dụng mạng Wi-Fi công cộng Hải Phòng: Hệ thống mạng Wi-Fi công cộng phục vụ miễn phí nhu cầu tra cứu thông tin chính sách, thực hiện dịch vụ công trực tuyến.',
    'Chào mừng Nhân dân đến với Nhà văn hóa Trung tâm Thị trấn Núi Đèo',
    'Chính quyền số Thành phố Thủy Nguyên tương lai',
    'Cẩm nang số tra cứu các thủ tục hành chính, quy hoạch không gian đô thị mới Thủy Nguyên.',
    '/banners/haiphong_culture_house.jpg',
    'https://dichvucong.haiphong.gov.vn',
    id,
    20.9258,
    106.6781,
    'ACTIVE',
    TRUE,
    'FPT Telecom Hải Phòng',
    'HP-TN-PON-9932',
    'Vũ Đức Thịnh (Quản lý NVH Núi Đèo)',
    '0936.789.012',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM administrative_units WHERE code = 'HP_TN_NUITHANH'
AND NOT EXISTS (SELECT 1 FROM wifi_locations WHERE slug = 'hp-tn-nvh04');

-- 5. Quét dọn toàn bộ chuỗi cũ nếu còn sót trong các cột thông tin
UPDATE wifi_locations
SET name = REPLACE(REPLACE(name, 'Tan Son Nhat', 'Hải Phòng'), 'Tân Sơn Nhất', 'Hải Phòng'),
    address = REPLACE(REPLACE(address, 'Tan Son Nhat', 'Hải Phòng'), 'Tân Sơn Nhất', 'Hải Phòng'),
    description = REPLACE(REPLACE(description, 'Tan Son Nhat', 'Hải Phòng'), 'Tân Sơn Nhất', 'Hải Phòng'),
    welcome_message = REPLACE(REPLACE(welcome_message, 'Tan Son Nhat', 'Hải Phòng'), 'Tân Sơn Nhất', 'Hải Phòng')
WHERE name ILIKE '%tan son nhat%' OR address ILIKE '%tan son nhat%' OR description ILIKE '%tan son nhat%' OR welcome_message ILIKE '%tan son nhat%';
