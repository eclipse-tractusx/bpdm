ALTER TABLE logistic_addresses DROP COLUMN site_external_id;

ALTER TABLE logistic_addresses DROP COLUMN phy_admin_area_l4;

ALTER TABLE logistic_addresses DROP COLUMN phy_district_l2;

ALTER TABLE legal_entities DROP COLUMN currentness;

-- Remove Regions Constraints
ALTER TABLE logistic_addresses
DROP CONSTRAINT IF EXISTS fk1msrc2opgg7y8hllv9mxydm41;

ALTER TABLE logistic_addresses
DROP CONSTRAINT IF EXISTS fkejpo9hh93uu777fbmmixh8v2f;

-- Alter columns from BIGINT to VARCHAR
ALTER TABLE logistic_addresses
ALTER COLUMN alt_admin_area_l1_region TYPE VARCHAR(255);

ALTER TABLE logistic_addresses
ALTER COLUMN phy_admin_area_l1_region TYPE VARCHAR(255);

-- Removal of Regions Table
DROP TABLE IF EXISTS regions;

-- Removal of Identifiers TYPE
DROP TABLE IF EXISTS legal_entity_identifiers CASCADE;

DROP TABLE IF EXISTS address_identifiers CASCADE;