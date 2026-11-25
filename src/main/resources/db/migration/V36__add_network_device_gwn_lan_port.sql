-- ==============================================================================
-- V36: Bổ sung cổng LAN trên router Grandstream GWN mà mesh cắm (gwn_lan_port).
--      Dùng để suy ra on/off CHÍNH XÁC khi nhiều mesh cắm cùng một GWN: đọc đúng
--      linkStatus cổng của từng mesh (device/info không cho biết MAC nào ở cổng nào,
--      nên phải khai cổng lúc đăng ký/import). Giá trị = số in trên vỏ cổng ("4","5","6").
-- ==============================================================================

ALTER TABLE network_devices
    ADD COLUMN IF NOT EXISTS gwn_lan_port VARCHAR(8);

-- Mesh demo VIETTEL_TEST cắm LAN4 của GWN7002 (đồng bộ với MESH_GWN_PORT_MAP hiện tại)
UPDATE network_devices SET gwn_lan_port = '4'
WHERE mac_address = '24:0B:2B:47:2A:22' AND gwn_lan_port IS NULL;
