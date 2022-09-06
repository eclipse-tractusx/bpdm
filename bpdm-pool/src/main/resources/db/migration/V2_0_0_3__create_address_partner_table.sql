CREATE TABLE address_partners
(
    id              BIGINT                      NOT NULL,
    uuid            UUID                        NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    bpn             VARCHAR(255)                NOT NULL,
    site_id         BIGINT,
    legal_entity_id BIGINT,
    address_id      BIGINT                      NOT NULL,
    CONSTRAINT pk_address_partners PRIMARY KEY (id)
);

ALTER TABLE address_partners
    ADD CONSTRAINT FK_ADDRESS_PARTNERS_ON_SITE FOREIGN KEY (site_id) REFERENCES sites (id);

ALTER TABLE address_partners
    ADD CONSTRAINT FK_ADDRESS_PARTNERS_ON_LEGAL_ENTITY FOREIGN KEY (legal_entity_id) REFERENCES business_partners (id);

ALTER TABLE address_partners
    ADD CONSTRAINT FK_ADDRESS_PARTNERS_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

ALTER TABLE addresses
    ALTER COLUMN bpn drop not null;


ALTER TABLE business_partners
    ADD COLUMN legal_address_id BIGINT;

ALTER TABLE business_partners
    ADD CONSTRAINT FK_BUSINESS_PARTNERS_ON_ADDRESSES FOREIGN KEY (legal_address_id) REFERENCES addresses (id);

ALTER TABLE sites
    ADD COLUMN main_address_id BIGINT;

ALTER TABLE sites
    ADD CONSTRAINT FK_SITES_ON_ADDRESSES FOREIGN KEY (main_address_id) REFERENCES addresses (id);

