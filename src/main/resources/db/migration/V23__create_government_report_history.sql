-- V23: Lưu vết lịch sử xuất và nộp báo cáo Sở TT&TT / UBND TP Hải Phòng
CREATE TABLE IF NOT EXISTS government_report_history (
    id BIGSERIAL PRIMARY KEY,
    report_code VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    period VARCHAR(32) NOT NULL,
    total_locations INT DEFAULT 1584,
    online_locations INT DEFAULT 0,
    sla_percentage DOUBLE PRECISION DEFAULT 0.0,
    total_sessions BIGINT DEFAULT 0,
    total_data_gb DOUBLE PRECISION DEFAULT 0.0,
    export_format VARCHAR(16) NOT NULL DEFAULT 'PDF',
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    submitted_by VARCHAR(100),
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    recipient_agency VARCHAR(255) DEFAULT 'UBND TP Hải Phòng & Sở TT&TT',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO government_report_history (report_code, title, period, total_locations, online_locations, sla_percentage, total_sessions, total_data_gb, export_format, status, submitted_by, recipient_agency, notes)
VALUES
('BC-HP-2026-10', 'Báo cáo định kỳ tháng 10/2026 - Hạ tầng Wi-Fi 1.584 Nhà văn hóa', 'PREV_MONTH', 1584, 1572, 99.24, 48210, 12450.5, 'PDF', 'SUBMITTED', 'sotttt@haiphong.gov.vn', 'UBND TP Hải Phòng & Sở TT&TT', 'Đã nộp thành công qua trục liên thông văn bản điện tử');

INSERT INTO government_report_history (report_code, title, period, total_locations, online_locations, sla_percentage, total_sessions, total_data_gb, export_format, status, submitted_by, recipient_agency, notes)
VALUES
('BC-HP-2026-Q3', 'Báo cáo tổng kết Quý 3/2026 - Mạng Wi-Fi công cộng phục vụ Chuyển đổi số', 'QUARTER', 1584, 1568, 98.98, 142300, 36800.0, 'PDF', 'SUBMITTED', 'sotttt@haiphong.gov.vn', 'Sở TT&TT Hải Phòng', 'Báo cáo tổng kết quý phục vụ kỳ họp HĐND Thành phố');
