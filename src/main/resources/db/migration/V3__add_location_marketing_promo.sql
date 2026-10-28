-- V3: Add promotional banner, survey, and marketing content fields to wifi_locations
ALTER TABLE wifi_locations ADD COLUMN IF NOT EXISTS promo_title VARCHAR(150);
ALTER TABLE wifi_locations ADD COLUMN IF NOT EXISTS promo_content TEXT;
ALTER TABLE wifi_locations ADD COLUMN IF NOT EXISTS promo_banner_url VARCHAR(500);
ALTER TABLE wifi_locations ADD COLUMN IF NOT EXISTS promo_action_url VARCHAR(500);

-- Seed marketing and survey samples for existing locations
UPDATE wifi_locations
SET promo_title = 'Khảo sát ý kiến khách hàng - Nhận voucher 20%',
    promo_content = 'Quý khách vui lòng dành 30 giây đánh giá chất lượng dịch vụ để nhận ngay mã giảm giá 20% cho lần ghé thăm tiếp theo!',
    promo_banner_url = 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=800&auto=format&fit=crop&q=80',
    promo_action_url = 'https://google.com'
WHERE slug = 'circle-k-central';

UPDATE wifi_locations
SET promo_title = 'Dịch vụ phòng chờ thương gia Wi-Fi Chùa',
    promo_content = 'Không gian làm việc yên tĩnh, đồ uống cao cấp và kết nối Internet tốc độ cao không giới hạn cho mọi chuyến bay.',
    promo_banner_url = 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=800&auto=format&fit=crop&q=80',
    promo_action_url = 'https://google.com'
WHERE slug = 'circle-k-airport';

UPDATE wifi_locations
SET promo_title = 'Thưởng thức Menu Mới - Ưu đãi 15%',
    promo_content = 'Trải nghiệm các món đặc trưng mới bên bờ sông thoáng đãng. Ưu đãi giảm ngay 15% cho khách sử dụng Wi-Fi.',
    promo_banner_url = 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800&auto=format&fit=crop&q=80',
    promo_action_url = 'https://google.com'
WHERE slug = 'circle-k-riverside';
