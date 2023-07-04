-- Description: Make 'legal_form_id' column optional in the 'legal_entities' table
-- Alter the 'legal_form_id' column to allow NULL values
ALTER TABLE legal_entities ALTER COLUMN legal_form_id DROP NOT NULL;
