ALTER TABLE changelog_entries
    ADD COLUMN data_type VARCHAR(255) NOT NULL DEFAULT 'Input';

