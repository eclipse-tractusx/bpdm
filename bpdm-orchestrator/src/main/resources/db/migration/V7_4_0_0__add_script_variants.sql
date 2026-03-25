CREATE TABLE business_partner_legal_entity_script_variants
(
    task_id                         BIGINT                    NOT NULL,
    script_code                     VARCHAR(255)              NOT NULL,
    legal_short_name                VARCHAR(255),
    legal_name                      VARCHAR(255),
    CONSTRAINT fk_legal_entity_script_variants_tasks FOREIGN KEY (task_id) REFERENCES golden_record_tasks(id)
);

CREATE TABLE business_partner_site_script_variants
(
    task_id                         BIGINT                    NOT NULL,
    script_code                     VARCHAR(255)              NOT NULL,
    site_name                       VARCHAR(255),
    CONSTRAINT fk_site_script_variants_tasks FOREIGN KEY (task_id) REFERENCES golden_record_tasks(id)
);

CREATE TABLE business_partner_address_script_variants
(
    task_id                         BIGINT                          NOT NULL,
    scope                           VARCHAR(255)                    NOT NULL,
    script_code                     VARCHAR(255)                    NOT NULL,
    address_name                    VARCHAR(255),
    phy_street_name                 VARCHAR(255),
    phy_house_number                VARCHAR(255),
    phy_house_number_supplement     VARCHAR(255),
    phy_milestone                   VARCHAR(255),
    phy_direction                   VARCHAR(255),
    phy_street_name_prefix                 VARCHAR(255),
    phy_street_name_additional_prefix      VARCHAR(255),
    phy_street_name_suffix                 VARCHAR(255),
    phy_street_name_additional_suffix      VARCHAR(255),
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
    CONSTRAINT fk_address_script_variants_tasks FOREIGN KEY (task_id) REFERENCES golden_record_tasks(id)
);

