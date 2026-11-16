-- V21: Quản lý lịch sử phiên bản giao diện Cổng chào
CREATE TABLE IF NOT EXISTS portal_template_versions (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    version_name VARCHAR(255),
    snapshot_json TEXT NOT NULL,
    created_by VARCHAR(100) DEFAULT 'admin',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_portal_template_version FOREIGN KEY (template_id) REFERENCES portal_templates (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_portal_template_version ON portal_template_versions (template_id, version_number DESC);

