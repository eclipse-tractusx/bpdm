ALTER TABLE IF EXISTS legal_entities
    ADD COLUMN legal_address_id BIGINT NULL;

ALTER TABLE IF EXISTS legal_entities
    ADD CONSTRAINT FK_ADDRESS_ON_LEGAL_ENTITY FOREIGN KEY (legal_address_id) REFERENCES logistic_addresses (id);