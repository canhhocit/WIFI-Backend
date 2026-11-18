-- ==============================================================================
-- V24: Cập nhật ràng buộc khóa ngoại cho chiến dịch truyền thông (campaigns)
-- Khi xóa chiến dịch:
-- 1. Tự động xóa lịch sử duyệt (campaign_approvals) qua ON DELETE CASCADE
-- 2. Tự động ngắt liên kết bài khảo sát (surveys.campaign_id -> NULL) qua ON DELETE SET NULL để bảo toàn dữ liệu khảo sát của nhân dân
-- ==============================================================================

ALTER TABLE campaign_approvals DROP CONSTRAINT IF EXISTS campaign_approvals_campaign_id_fkey;
ALTER TABLE campaign_approvals ADD CONSTRAINT campaign_approvals_campaign_id_fkey
    FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE;

ALTER TABLE surveys DROP CONSTRAINT IF EXISTS surveys_campaign_id_fkey;
ALTER TABLE surveys ADD CONSTRAINT surveys_campaign_id_fkey
    FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE SET NULL;
