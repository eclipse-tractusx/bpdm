CREATE TABLE roles (
  id BIGINT NOT NULL,
  created_at TIMESTAMP WITH time zone NOT NULL,
  updated_at TIMESTAMP WITH time zone NOT NULL,
  uuid UUID NOT NULL,
  address_id BIGINT NULL,
  site_id BIGINT NULL,
  legal_entity_id BIGINT NULL,
  role_name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX legal_entity_roles ON roles (legal_entity_id);

ALTER TABLE IF EXISTS roles ADD CONSTRAINT uuid_roles_uk UNIQUE (uuid);

ALTER TABLE IF EXISTS roles
ADD CONSTRAINT fk_legal_entity_roles FOREIGN KEY (legal_entity_id) REFERENCES legal_entities;

ALTER TABLE logistic_addresses
ADD COLUMN IF NOT EXISTS phy_name_prefix VARCHAR(255),
ADD COLUMN IF NOT EXISTS phy_additional_name_prefix VARCHAR(255),
ADD COLUMN IF NOT EXISTS phy_name_suffix VARCHAR(255),
ADD COLUMN IF NOT EXISTS phy_additional_name_suffix VARCHAR(255);

