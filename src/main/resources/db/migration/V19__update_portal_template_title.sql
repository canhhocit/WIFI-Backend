-- Migration V19: Update default portal template branding to Wifi công cộng Hải Phòng
UPDATE portal_templates
SET name = 'Giao diện Tiêu chuẩn Wifi công cộng Hải Phòng',
    title = 'Cổng Thông Tin Wi-Fi Hải Phòng',
    updated_at = CURRENT_TIMESTAMP
WHERE template_type = 'CULTURAL';
