-- ==============================================================================
-- V11: Làm sạch 100% tàn dư dữ liệu Tân Sơn Nhất trong Audit Logs
--      & Bổ sung trường background_image_url cho Cổng chào (Portal Templates & Wi-Fi Locations)
-- ==============================================================================

-- 1. Làm sạch triệt để nhật ký kiểm toán (Audit Events)
UPDATE audit_events
SET details = REPLACE(details, 'Airport Gateway - Ga Đến Quốc Tế Tân Sơn Nhất', 'Nhà văn hóa Khu dân cư Máy Tơ - Quận Ngô Quyền')
WHERE details LIKE '%Airport Gateway%';

UPDATE audit_events
SET details = REPLACE(details, 'Airport Gateway', 'Nhà văn hóa Khu dân cư Máy Tơ')
WHERE details LIKE '%Airport Gateway%';

UPDATE audit_events
SET details = REPLACE(details, 'Tan Son Nhat', 'Hải Phòng')
WHERE details LIKE '%Tan Son Nhat%';

UPDATE audit_events
SET details = REPLACE(details, 'Tân Sơn Nhất', 'Hải Phòng')
WHERE details LIKE '%Tân Sơn Nhất%';

-- 2. Bổ sung trường hình nền trang Cổng chào (background_image_url) cho portal_templates
ALTER TABLE portal_templates
    ADD COLUMN IF NOT EXISTS background_image_url VARCHAR(500) DEFAULT '/banners/haiphong_city_bg.jpg';

UPDATE portal_templates
SET background_image_url = '/banners/haiphong_city_bg.jpg'
WHERE background_image_url IS NULL OR background_image_url = '';

-- 3. Bổ sung trường hình nền Cổng chào riêng từng điểm cho wifi_locations
ALTER TABLE wifi_locations
    ADD COLUMN IF NOT EXISTS background_image_url VARCHAR(500) DEFAULT '/banners/haiphong_city_bg.jpg';

UPDATE wifi_locations
SET background_image_url = '/banners/haiphong_city_bg.jpg'
WHERE background_image_url IS NULL OR background_image_url = '';
