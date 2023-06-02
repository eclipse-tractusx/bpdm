-- Remove the existing unique constraint on external_id (Addresses)
ALTER TABLE logistic_addresses
DROP CONSTRAINT IF EXISTS uk_7xolefhhm30nlfrp5fc25a3i2;

-- Add the composite unique constraint on external_id and data_type (Addresses)
ALTER TABLE logistic_addresses
ADD COLUMN data_type VARCHAR(255) NOT NULL,
ADD CONSTRAINT uk_external_id_data_type UNIQUE (external_id, data_type);

-- Remove the existing unique constraint on external_id (Site)
ALTER TABLE sites
DROP CONSTRAINT IF EXISTS UK_1vrdeiex4x7p93r5svtvb5b4x;

-- Add the composite unique constraint on external_id and data_type (Site)
ALTER TABLE sites
ADD COLUMN data_type VARCHAR(255) NOT NULL,
ADD CONSTRAINT uk_external_id_data_type_site UNIQUE (external_id, data_type);

-- Remove the existing unique constraint on external_id (Site)
ALTER TABLE legal_entities
DROP CONSTRAINT IF EXISTS uk_fn3cbtgn4mcc8qprvf6nc33rb;

-- Add the composite unique constraint on external_id and data_type (Site)
ALTER TABLE legal_entities
ADD COLUMN data_type VARCHAR(255) NOT NULL,
ADD CONSTRAINT uk_external_id_data_type_legalentity UNIQUE (external_id, data_type);