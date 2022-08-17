CREATE TABLE partner_addresses
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    bpn        VARCHAR(255)                NOT NULL,
    site_id    BIGINT,
    partner_id BIGINT,
    address_id BIGINT                      NOT NULL,
    CONSTRAINT pk_partner_addresses PRIMARY KEY (id)
);

ALTER TABLE addresses
    ALTER COLUMN bpn drop not null;

ALTER TABLE partner_addresses
    ADD CONSTRAINT FK_PARTNER_ADDRESSES_ON_PARTNER FOREIGN KEY (partner_id) REFERENCES business_partners (id);

ALTER TABLE partner_addresses
    ADD CONSTRAINT FK_PARTNER_ADDRESSES_ON_SITE FOREIGN KEY (site_id) REFERENCES sites (id);

ALTER TABLE business_partners
    ADD COLUMN legal_address_id BIGINT;

ALTER TABLE business_partners
    ADD CONSTRAINT FK_BUSINESS_PARTNERS_ON_ADDRESSES FOREIGN KEY (legal_address_id) REFERENCES addresses (id);

ALTER TABLE sites
    ADD COLUMN main_address_id BIGINT;

ALTER TABLE sites
    ADD CONSTRAINT FK_SITES_ON_ADDRESSES FOREIGN KEY (main_address_id) REFERENCES addresses (id);

