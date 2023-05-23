
CREATE TABLE address_identifiers (
  id BIGINT NOT NULL,
  created_at TIMESTAMP WITH time zone NOT NULL,
  updated_at TIMESTAMP WITH time zone NOT NULL,
  uuid UUID NOT NULL,
  "value" VARCHAR(255) NOT NULL,
  identifier_type_key VARCHAR(255) NOT NULL,
  address_id BIGINT NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE address_states (
  id BIGINT NOT NULL,
  created_at TIMESTAMP WITH time zone NOT NULL,
  updated_at TIMESTAMP WITH time zone NOT NULL,
  uuid UUID NOT NULL,
  description VARCHAR(255),
  type VARCHAR(255) NOT NULL,
  valid_from TIMESTAMP,
  valid_to TIMESTAMP,
  address_id BIGINT NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE logistic_addresses (
  id BIGINT NOT NULL,
  created_at TIMESTAMP WITH time zone NOT NULL,
  updated_at TIMESTAMP WITH time zone NOT NULL,
  uuid UUID NOT NULL,
  alt_admin_area_l2 VARCHAR(255),
  alt_admin_area_l3 VARCHAR(255),
  alt_admin_area_l4 VARCHAR(255),
  alt_city VARCHAR(255),
  alt_country VARCHAR(255),
  alt_delivery_service_number VARCHAR(255),
  alt_delivery_service_type VARCHAR(255),
  alt_district_l1 VARCHAR(255),
  alt_district_l2 VARCHAR(255),
  alt_altitude FLOAT4,
  alt_latitude FLOAT4,
  alt_longitude FLOAT4,
  alt_postcode VARCHAR(255),
  alt_street_direction VARCHAR(255),
  alt_street_number VARCHAR(255),
  alt_street_milestone VARCHAR(255),
  alt_street_name VARCHAR(255),
  bpn VARCHAR(255) NULL,
  external_id VARCHAR(255) NOT NULL,
  legal_entity_external_id VARCHAR(255),
  NAME VARCHAR(255),
  phy_admin_area_l2 VARCHAR(255),
  phy_admin_area_l3 VARCHAR(255),
  phy_admin_area_l4 VARCHAR(255),
  phy_building VARCHAR(255),
  phy_city VARCHAR(255),
  phy_country VARCHAR(255),
  phy_district_l1 VARCHAR(255),
  phy_district_l2 VARCHAR(255),
  phy_door VARCHAR(255),
  phy_floor VARCHAR(255),
  phy_altitude FLOAT4,
  phy_latitude FLOAT4,
  phy_longitude FLOAT4,
  phy_industrial_zone VARCHAR(255),
  phy_postcode VARCHAR(255),
  phy_company_postcode VARCHAR(255),
  phy_street_direction VARCHAR(255),
  phy_street_number VARCHAR(255),
  phy_street_milestone VARCHAR(255),
  phy_street_name VARCHAR(255),
  site_external_id VARCHAR(255),
  alt_admin_area_l1_region BIGINT,
  phy_admin_area_l1_region BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE regions (
  id BIGINT NOT NULL,
  created_at TIMESTAMP WITH time zone NOT NULL,
  updated_at TIMESTAMP WITH time zone NOT NULL,
  uuid UUID NOT NULL,
  country_code VARCHAR(255),
  region_code VARCHAR(255),
  region_name VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE INDEX idxt1nfv727tjqujqr7b0hxggbq5 ON address_identifiers (address_id);

ALTER TABLE IF EXISTS address_identifiers
ADD CONSTRAINT uk_rcrdyqidxxabhh6hgajyd5g9b UNIQUE (uuid);

CREATE INDEX idx6nuoy4ynerttj0kmrx9h0o9w1 ON address_states (address_id);

ALTER TABLE IF EXISTS address_states
ADD CONSTRAINT uk_2hleh9jm9ef1eq6851bp30v1r UNIQUE (uuid);

ALTER TABLE IF EXISTS logistic_addresses
ADD CONSTRAINT uk_fbtqvm8vt1nx20nxlr0ixpsi8 UNIQUE (uuid);

ALTER TABLE IF EXISTS logistic_addresses
ADD CONSTRAINT uk_7xolefhhm30nlfrp5fc25a3i2 UNIQUE (external_id);

ALTER TABLE IF EXISTS regions
ADD CONSTRAINT uk_po9lr0ewg38m4xhdaxo9t2hmt UNIQUE (uuid);

ALTER TABLE IF EXISTS address_identifiers
ADD CONSTRAINT fkfiidrdbv4um8eaxwdb7737cs5 FOREIGN KEY (address_id) REFERENCES logistic_addresses;

ALTER TABLE IF EXISTS address_states
ADD CONSTRAINT fk6mrebbkx9qe0mnbi9fxrj0d0j FOREIGN KEY (address_id) REFERENCES logistic_addresses;

ALTER TABLE IF EXISTS logistic_addresses
ADD CONSTRAINT fk1msrc2opgg7y8hllv9mxydm41 FOREIGN KEY (alt_admin_area_l1_region) REFERENCES regions;

ALTER TABLE IF EXISTS logistic_addresses
ADD CONSTRAINT fkejpo9hh93uu777fbmmixh8v2f FOREIGN KEY (phy_admin_area_l1_region) REFERENCES regions;

