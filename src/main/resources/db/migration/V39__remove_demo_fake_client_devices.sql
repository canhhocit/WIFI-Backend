-- Xoá dữ liệu DEMO thiết bị khách (và phiên) được seed cứng ở V2__seed_demo_data.sql,
-- để tab "Thiết bị khách truy cập" chỉ còn hiển thị thiết bị THẬT đi qua cổng chào.
-- 3 MAC giả: iPhone-Guest, Galaxy-S24, MacBookPro-Guest.
DELETE FROM guest_sessions
 WHERE mac_address IN ('a4:83:e7:2b:41:90', '5c:f9:38:91:a2:14', '88:66:5a:33:cb:7e');

DELETE FROM devices
 WHERE mac_address IN ('a4:83:e7:2b:41:90', '5c:f9:38:91:a2:14', '88:66:5a:33:cb:7e');
