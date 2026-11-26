-- Input routing and output provenance must survive process switches independently.
ALTER TABLE sharing_states ADD COLUMN process TEXT;
ALTER TABLE business_partners ADD COLUMN process TEXT;
ALTER TABLE changelog_entries ADD COLUMN process TEXT;
