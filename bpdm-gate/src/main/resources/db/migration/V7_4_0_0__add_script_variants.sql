CREATE TABLE business_partner_script_variants
(
    id                              BIGINT                          NOT NULL,
    uuid                            UUID                            NOT NULL,
    created_at                      TIMESTAMP WITHOUT TIME ZONE     NOT NULL,
    updated_at                      TIMESTAMP WITHOUT TIME ZONE     NOT NULL,
    script_code                     VARCHAR(255)                    NOT NULL,
    business_partner_id             BIGINT                          NOT NULL,
    short_name                      VARCHAR(255),
    legal_name                      VARCHAR(255),
    site_name                       VARCHAR(255),
    address_name                    VARCHAR(255),
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
    CONSTRAINT pk_script_variants PRIMARY KEY (id),
    CONSTRAINT fk_script_variants_business_partners FOREIGN KEY (business_partner_id) REFERENCES business_partners(id),
    CONSTRAINT uc_script_code_business_partner UNIQUE (script_code, business_partner_id)
);

CREATE TABLE business_partner_script_variant_name_parts
(
    script_variant_id               BIGINT                          NOT NULL,
    name_part                       VARCHAR(255)                    NOT NULL,
    name_parts_order                INTEGER                         NOT NULL,
    CONSTRAINT fk_script_variant_name_parts_variants FOREIGN KEY (script_variant_id) REFERENCES business_partner_script_variants(id),
    CONSTRAINT uc_script_variant_name_parts UNIQUE (script_variant_id, name_part, name_parts_order)
);