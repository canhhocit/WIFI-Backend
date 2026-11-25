-- V38: Lưu trực tiếp đơn vị / bộ phận tiếp nhận xử lý sự cố (Sở KH&CN, UBND Xã/Phường, Quản trị hệ thống...)
ALTER TABLE incident_tickets
    ADD COLUMN IF NOT EXISTS assigned_group VARCHAR(150);
