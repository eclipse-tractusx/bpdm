ALTER TABLE IF EXISTS logistic_addresses ADD legal_entity_id int8 NULL;

ALTER TABLE IF EXISTS logistic_addresses
    ADD CONSTRAINT FK_LEGAL_ENTITY_ON_ADDRESS FOREIGN KEY (legal_entity_id) REFERENCES legal_entities (id);