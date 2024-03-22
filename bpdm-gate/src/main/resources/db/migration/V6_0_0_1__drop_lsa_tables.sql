DROP TABLE address_identifiers;
DROP TABLE address_states;
DROP TABLE classifications;
DROP TABLE legal_entity_identifiers;
DROP TABLE legal_entity_states;
DROP TABLE name_parts;
DROP TABLE relations;
DROP TABLE site_states;
DROP TABLE roles;

ALTER TABLE legal_entities
DROP CONSTRAINT FK_ADDRESS_ON_LEGAL_ENTITY;

ALTER TABLE sites
DROP CONSTRAINT FK_MAIN_ADDRESS_ON_SITES;

DROP TABLE logistic_addresses;
DROP TABLE sites;
DROP TABLE legal_entities;
