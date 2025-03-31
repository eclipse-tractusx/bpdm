ALTER TABLE business_partner_relations
ADD COLUMN output_relation_type VARCHAR(255),
ADD COLUMN output_source_bpnl VARCHAR(255),
ADD COLUMN output_target_bpnl VARCHAR(255),
ADD COLUMN output_updated_at TIMESTAMP WITHOUT TIME ZONE;