-- Tổng số cổng LAN RJ45 (role=0, không phải SFP) của router GWN tại điểm, do scheduler nền cập nhật.
-- Dùng để tính "kỳ vọng = min(4, số cổng thật)" nhất quán giữa thanh tổng quan và panel AP theo cổng.
ALTER TABLE wifi_locations ADD COLUMN IF NOT EXISTS ap_total_count INTEGER;
