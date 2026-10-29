-- ==============================================================================
-- V7: Network QoS Bandwidth Policies, MAC Blacklist, 12-Month Access Logs Retention & RADIUS Clients
-- ==============================================================================

-- 1. Network Bandwidth QoS Policies
CREATE TABLE network_bandwidth_policies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    download_speed_kbps INTEGER NOT NULL DEFAULT 5120,
    upload_speed_kbps INTEGER NOT NULL DEFAULT 2048,
    max_session_seconds INTEGER NOT NULL DEFAULT 5400,
    quota_mb INTEGER DEFAULT 0,
    priority INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_network_policies_active ON network_bandwidth_policies(active);

-- Seed default QoS Policies
INSERT INTO network_bandwidth_policies (name, description, download_speed_kbps, upload_speed_kbps, max_session_seconds, quota_mb, priority, active)
VALUES 
('Chính sách Tiêu chuẩn NVH (Standard)', 'Băng thông công cộng tiêu chuẩn 5Mbps tải xuống, 2Mbps tải lên cho 1.584 Nhà văn hóa', 5120, 2048, 5400, 0, 1, TRUE),
('Chính sách Ưu tiên Cán bộ (VIP / Officer)', 'Băng thông ưu tiên 20Mbps tải xuống, 10Mbps tải lên phục vụ hội nghị và điều hành số', 20480, 10240, 14400, 0, 5, TRUE),
('Chính sách Tiết kiệm Băng thông (Eco)', 'Giới hạn 2Mbps/1Mbps cho các điểm vùng sâu, hải đảo điều kiện truyền dẫn cáp hạn chế', 2048, 1024, 3600, 500, 2, TRUE);

-- 2. MAC Address Blacklist (Bảo đảm an ninh mạng & chặn thiết bị vi phạm)
CREATE TABLE mac_blacklist (
    id BIGSERIAL PRIMARY KEY,
    mac_address VARCHAR(17) NOT NULL UNIQUE,
    reason VARCHAR(255) NOT NULL,
    blocked_by VARCHAR(100) NOT NULL,
    blocked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_mac_blacklist_active ON mac_blacklist(active);

-- Seed initial blocked sample
INSERT INTO mac_blacklist (mac_address, reason, blocked_by, active)
VALUES ('AA:BB:CC:DD:EE:00', 'Quét cổng bất hợp pháp và giả lập địa chỉ IP mạng nội bộ', 'admin', TRUE);

-- 3. Legal Access Logs Retention (Quy định lưu trữ nhật ký truy cập 12 tháng theo Luật An ninh mạng & Nghị định viễn thông)
CREATE TABLE access_logs_retention (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT,
    session_token VARCHAR(100),
    client_mac VARCHAR(17) NOT NULL,
    client_ip VARCHAR(45) NOT NULL,
    location_id BIGINT,
    location_name VARCHAR(150),
    phone_number VARCHAR(20),
    authenticated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    disconnected_at TIMESTAMP WITH TIME ZONE,
    duration_seconds BIGINT DEFAULT 0,
    bytes_in BIGINT DEFAULT 0,
    bytes_out BIGINT DEFAULT 0,
    termination_reason VARCHAR(50) DEFAULT 'USER_LOGOUT',
    retained_until TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_retention_client_mac ON access_logs_retention(client_mac);
CREATE INDEX idx_retention_authenticated_at ON access_logs_retention(authenticated_at);
CREATE INDEX idx_retention_retained_until ON access_logs_retention(retained_until);
CREATE INDEX idx_retention_location_id ON access_logs_retention(location_id);

-- 4. RADIUS Clients Configuration (OPNsense / Mikrotik NAS Clients)
CREATE TABLE radius_clients (
    id BIGSERIAL PRIMARY KEY,
    client_name VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    shared_secret VARCHAR(100) NOT NULL,
    nas_identifier VARCHAR(100),
    description VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed sample RADIUS gateway
INSERT INTO radius_clients (client_name, ip_address, shared_secret, nas_identifier, description, enabled)
VALUES 
('OPNsense Gateway Hải Phòng Trung tâm', '192.168.100.1', 'hp_radius_secret_2026', 'OPNSENSE-CORE-HP', 'Gateway Captive Portal lõi đặt tại Trung tâm tích hợp dữ liệu TP Hải Phòng', TRUE),
('Mikrotik Gateway Đảo Cát Hải', '192.168.200.1', 'cat_hai_radius_2026', 'MIKROTIK-CAT-HAI', 'Gateway phân vùng viễn thông huyện đảo Cát Hải', TRUE);
