ALTER TABLE business_partner_relations
ADD COLUMN output_valid_from TIMESTAMP,
ADD COLUMN output_valid_to TIMESTAMP;

ALTER TABLE business_partner_relation_stages
ADD COLUMN valid_from TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT '1970-01-01T00:00:00Z';

ALTER TABLE business_partner_relation_stages
ADD COLUMN valid_to TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT '9999-12-31 23:59:59';
