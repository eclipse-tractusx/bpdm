CREATE TABLE script_codes
(
    id              BIGINT                      NOT NULL,
    uuid            UUID                        NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    technical_key   VARCHAR(255)                NOT NULL,
    description     VARCHAR(255),
    CONSTRAINT pk_script_codes PRIMARY KEY (id),
    CONSTRAINT uc_script_codes_technical_key UNIQUE (technical_key)
);

CREATE TABLE legal_entity_script_variants
(
    script_code_id                  BIGINT                    NOT NULL,
    legal_entity_id                 BIGINT                    NOT NULL,
    short_name                      VARCHAR(255),
    legal_name                      VARCHAR(255),
    CONSTRAINT fk_le_script_variants_script_codes FOREIGN KEY (script_code_id) REFERENCES script_codes(id),
    CONSTRAINT fk_script_variants_legal_entities FOREIGN KEY (legal_entity_id) REFERENCES legal_entities(id),
    CONSTRAINT uc_script_code_legal_entity UNIQUE (script_code_id, legal_entity_id)
);

CREATE TABLE site_script_variants
(
    script_code_id                  BIGINT                          NOT NULL,
    site_id                         BIGINT                          NOT NULL,
    name                            VARCHAR(255),
    CONSTRAINT fk_site_script_variants_script_codes FOREIGN KEY (script_code_id) REFERENCES script_codes(id),
    CONSTRAINT fk_script_variants_sites FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT uc_script_code_site UNIQUE (script_code_id, site_id)
);

CREATE TABLE address_script_variants
(
    script_code_id                  BIGINT                          NOT NULL,
    logistic_address_id             BIGINT                          NOT NULL,
    name                            VARCHAR(255),
    phy_street_name                 VARCHAR(255),
    phy_street_number               VARCHAR(255),
    phy_street_number_supplement    VARCHAR(255),
    phy_street_milestone            VARCHAR(255),
    phy_street_direction            VARCHAR(255),
    phy_name_prefix                 VARCHAR(255),
    phy_additional_name_prefix      VARCHAR(255),
    phy_name_suffix                 VARCHAR(255),
    phy_additional_name_suffix      VARCHAR(255),
    phy_postcode                    VARCHAR(255),
    phy_city                        VARCHAR(255),
    phy_district_l1                 VARCHAR(255),
    phy_company_postcode            VARCHAR(255),
    phy_industrial_zone             VARCHAR(255),
    phy_building                    VARCHAR(255),
    phy_floor                       VARCHAR(255),
    phy_door                        VARCHAR(255),
    phy_tax_jurisdiction            VARCHAR(255),
    alt_postcode                    VARCHAR(255),
    alt_city                        VARCHAR(255),
    alt_delivery_service_qualifier  VARCHAR(255),
    alt_delivery_service_number     VARCHAR(255),
    CONSTRAINT fk_address_script_variants_script_codes FOREIGN KEY (script_code_id) REFERENCES script_codes(id),
    CONSTRAINT fk_script_variants_addresses FOREIGN KEY (logistic_address_id) REFERENCES logistic_addresses(id),
    CONSTRAINT uc_script_code_address UNIQUE (script_code_id, logistic_address_id)
);

INSERT INTO script_codes (id, uuid, created_at, updated_at, technical_key, description)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), now(), now(), 'test', 'test description');