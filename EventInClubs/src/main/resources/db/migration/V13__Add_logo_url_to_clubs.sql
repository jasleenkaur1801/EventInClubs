-- Add logo_url column to clubs table
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500);
