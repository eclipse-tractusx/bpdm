ALTER TABLE golden_record_tasks
	ADD COLUMN legal_entity_updated_at TIMESTAMP,
	ADD COLUMN site_updated_at TIMESTAMP;

ALTER TABLE business_partner_addresses
	ADD COLUMN updated_at TIMESTAMP;

