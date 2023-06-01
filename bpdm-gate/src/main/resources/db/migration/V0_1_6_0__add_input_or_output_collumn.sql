-- ALTER TABLE logistic_addresses ADD COLUMN data_type varchar(255) NOT NULL,
-- ADD CONSTRAINT uk_data_type UNIQUE (data_type);

-- Remove the existing unique constraint on external_id
ALTER TABLE logistic_addresses
DROP CONSTRAINT IF EXISTS uk_7xolefhhm30nlfrp5fc25a3i2;

-- Add the composite unique constraint on external_id and data_type
ALTER TABLE logistic_addresses
ADD COLUMN data_type VARCHAR(255) NOT NULL,
ADD CONSTRAINT uk_external_id_data_type UNIQUE (external_id, data_type);