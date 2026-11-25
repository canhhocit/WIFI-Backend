-- Xoá dữ liệu DEMO thiết bị khách (và dữ liệu liên quan) được seed cứng ở V2__seed_demo_data.sql,
-- để tab "Thiết bị khách truy cập" chỉ còn hiển thị thiết bị THẬT đi qua cổng chào.
-- 3 MAC giả: iPhone-Guest, Galaxy-S24, MacBookPro-Guest.
-- Xoá theo ĐÚNG THỨ TỰ KHÓA NGOẠI: gateway_transactions -> guest_sessions -> devices.

-- 1) Giao dịch gateway của các phiên demo (FK gateway_transactions.session_id -> guest_sessions.id)
DELETE FROM gateway_transactions
 WHERE session_id IN (
     SELECT id FROM guest_sessions
      WHERE mac_address IN ('a4:83:e7:2b:41:90', '5c:f9:38:91:a2:14', '88:66:5a:33:cb:7e')
 );

-- 2) Phiên cổng chào demo (FK guest_sessions.device_id -> devices.id)
DELETE FROM guest_sessions
 WHERE mac_address IN ('a4:83:e7:2b:41:90', '5c:f9:38:91:a2:14', '88:66:5a:33:cb:7e')
    OR device_id IN (
        SELECT id FROM devices
         WHERE mac_address IN ('a4:83:e7:2b:41:90', '5c:f9:38:91:a2:14', '88:66:5a:33:cb:7e')
    );

-- 3) Thiết bị khách demo
DELETE FROM devices
 WHERE mac_address IN ('a4:83:e7:2b:41:90', '5c:f9:38:91:a2:14', '88:66:5a:33:cb:7e');
