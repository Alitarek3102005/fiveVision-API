ALTER TABLE media_assets ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE media_assets ADD COLUMN deleted_by UUID REFERENCES users(id);
CREATE INDEX idx_media_assets_deleted_at ON media_assets(deleted_at);