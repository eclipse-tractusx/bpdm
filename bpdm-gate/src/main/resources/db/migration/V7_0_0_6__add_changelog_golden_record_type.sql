ALTER TABLE changelog_entries
ADD COLUMN golden_record_type VARCHAR(255) DEFAULT 'BusinessPartner' NOT NULL;