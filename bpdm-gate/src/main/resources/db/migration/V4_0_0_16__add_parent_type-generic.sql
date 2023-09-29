ALTER TABLE business_partners
ADD COLUMN parent_id VARCHAR(255),
ADD COLUMN parent_type VARCHAR(255);

ALTER TABLE changelog_entries
ALTER COLUMN business_partner_type DROP NOT NULL;