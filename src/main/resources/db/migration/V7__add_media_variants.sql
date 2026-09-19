ALTER TABLE media_assets
  ADD COLUMN thumbnail_key VARCHAR(500),
  ADD COLUMN thumbnail_url VARCHAR(1000),
  ADD COLUMN large_key VARCHAR(500),
  ADD COLUMN large_url VARCHAR(1000);