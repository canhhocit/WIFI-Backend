-- V22: Quản lý nhóm điểm tùy biến (Custom Location Groups)
CREATE TABLE IF NOT EXISTS location_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    color VARCHAR(20) DEFAULT '#0284c7',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS location_group_members (
    group_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, location_id),
    CONSTRAINT fk_group_member_group FOREIGN KEY (group_id) REFERENCES location_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_member_location FOREIGN KEY (location_id) REFERENCES wifi_locations(id) ON DELETE CASCADE
);

-- Seed các nhóm thực tế của TP. Hải Phòng
INSERT INTO location_groups (name, code, description, color) VALUES
('Điểm Trọng Điểm Du Lịch & Lễ Hội', 'TOURISM_FESTIVAL', 'Các nhà văn hóa gần điểm tham quan, danh thắng, di tích lịch sử', '#0ea5e9');

INSERT INTO location_groups (name, code, description, color) VALUES
('Địa Bàn Hải Đảo & Ven Biển', 'ISLAND_COASTAL', 'Các xã đảo Cát Hải, Bạch Long Vĩ và vùng ven biển', '#6366f1');

INSERT INTO location_groups (name, code, description, color) VALUES
('Thí Điểm Chuyển Đổi Số Nông Thôn Mới', 'DIGITAL_RURAL', 'Các nhà văn hóa mẫu hỗ trợ giải quyết Dịch vụ công trực tuyến', '#10b981');
