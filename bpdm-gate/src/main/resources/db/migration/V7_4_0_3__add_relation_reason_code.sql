ALTER TABLE business_partner_relation_stages
ADD COLUMN reason_code VARCHAR(255);

ALTER TABLE business_partner_relations
ADD COLUMN output_reason_code VARCHAR(255);