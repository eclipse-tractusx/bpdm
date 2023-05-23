ALTER TABLE IF EXISTS logistic_addresses ADD site_id int8 NULL;

ALTER TABLE IF EXISTS logistic_addresses
    ADD CONSTRAINT FK_SITE_ON_ADDRESS FOREIGN KEY (site_id) REFERENCES sites (id);


ALTER TABLE IF EXISTS sites ADD legal_entity_id int8 NOT NULL;

ALTER TABLE IF EXISTS sites
    ADD CONSTRAINT FK_LEGAL_ENTITY_ON_SITES FOREIGN KEY (legal_entity_id) REFERENCES legal_entities (id);


ALTER TABLE IF EXISTS sites ADD main_address_id int8 NOT NULL;

ALTER TABLE IF EXISTS sites
    ADD CONSTRAINT FK_MAIN_ADDRESS_ON_SITES FOREIGN KEY (main_address_id) REFERENCES logistic_addresses (id);