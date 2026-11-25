-- Số cổng LAN đang ON (= số AP thực đang kết nối) của router GWN tại điểm, do scheduler nền cập nhật.
-- Dùng để hiển thị "AP: X/4" và tính màu sức khỏe mà không phải gọi GWN mỗi lần mở danh sách (scale 1500 điểm).
ALTER TABLE wifi_locations ADD COLUMN IF NOT EXISTS active_ap_count INTEGER;
