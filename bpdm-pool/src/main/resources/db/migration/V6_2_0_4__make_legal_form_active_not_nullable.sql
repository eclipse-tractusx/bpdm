UPDATE legal_forms
SET is_active = false
WHERE is_active IS NULL;

ALTER TABLE legal_forms
ALTER COLUMN is_active SET NOT NULL;