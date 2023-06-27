ALTER TABLE logistic_addresses
DROP COLUMN IF EXISTS name;

ALTER TABLE sites
DROP COLUMN IF EXISTS name;

CREATE TABLE name_parts (
  id BIGINT NOT NULL,
  created_at TIMESTAMP WITH time zone NOT NULL,
  updated_at TIMESTAMP WITH time zone NOT NULL,
  uuid UUID NOT NULL,
  address_id BIGINT NULL,
  site_id BIGINT NULL,
  legal_entity_id BIGINT NULL,
  name_part VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX address_index_name_parts ON name_parts (address_id);

CREATE INDEX site_index_name_parts ON name_parts (site_id);

CREATE INDEX legal_entity_name_parts ON name_parts (legal_entity_id);

ALTER TABLE IF EXISTS name_parts
ADD CONSTRAINT uuid_name_parts_uk UNIQUE (uuid);

ALTER TABLE IF EXISTS name_parts
ADD CONSTRAINT fk_address_name_parts FOREIGN KEY (address_id) REFERENCES logistic_addresses,
ADD CONSTRAINT fk_legal_entity_name_parts FOREIGN KEY (legal_entity_id) REFERENCES legal_entities,
ADD CONSTRAINT fk_sites_name_parts FOREIGN KEY (site_id) REFERENCES sites;
