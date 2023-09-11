-- Create table for Address entity
CREATE TABLE postal_addresses
(
    id                             bigint                   NOT NULL,
    created_at                     TIMESTAMP WITH time zone NOT NULL,
    updated_at                     TIMESTAMP WITH time zone NOT NULL,
    uuid                           UUID                     NOT NULL,
    address_type                   varchar(255),
    phy_latitude                   double precision,
    phy_longitude                  double precision,
    phy_altitude                   double precision,
    phy_country                    varchar(255)             not null,
    phy_admin_area_l1_region       varchar(255),
    phy_admin_area_l2              varchar(255),
    phy_admin_area_l3              varchar(255),
    phy_postcode                   varchar(255),
    phy_city                       varchar(255)             not null,
    phy_district_l1                varchar(255),
    phy_street_name                varchar(255),
    phy_street_number              varchar(255),
    phy_street_milestone           varchar(255),
    phy_street_direction           varchar(255),
    phy_name_prefix                varchar(255),
    phy_additional_name_prefix     varchar(255),
    phy_name_suffix                varchar(255),
    phy_additional_name_suffix     varchar(255),
    phy_company_postcode           varchar(255),
    phy_industrial_zone            varchar(255),
    phy_building                   varchar(255),
    phy_floor                      varchar(255),
    phy_door                       varchar(255),
    alt_latitude                   double precision,
    alt_longitude                  double precision,
    alt_altitude                   double precision,
    alt_country                    varchar(255),
    alt_admin_area_l1_region       varchar(255),
    alt_postcode                   varchar(255),
    alt_city                       varchar(255),
    alt_delivery_service_type      varchar(255),
    alt_delivery_service_qualifier varchar(255),
    alt_delivery_service_number    varchar(255),
    PRIMARY KEY (id)
);

-- Create table for Business_Partner entity
CREATE TABLE business_partners
(
    id                bigint                   NOT NULL,
    created_at        TIMESTAMP WITH time zone NOT NULL,
    updated_at        TIMESTAMP WITH time zone NOT NULL,
    uuid              UUID                     NOT NULL,
    external_id       VARCHAR(255)             NOT NULL,
    short_name        VARCHAR(255),
    legal_form        VARCHAR(255),
    is_owner          BOOLEAN,
    address_bpn       VARCHAR(255),
    legal_entity_bpn  VARCHAR(255),
    site_bpn          VARCHAR(255),
    postal_address_id bigint                   NOT NULL UNIQUE,
    stage             VARCHAR(255),
    CONSTRAINT pk_business_partners PRIMARY KEY (id),
    FOREIGN KEY (postal_address_id) REFERENCES postal_addresses (id)
);

-- Create table for Name Parts
CREATE TABLE business_partners_name_parts
(
    name_parts_order    bigint,
    name_parts          VARCHAR(255),
    business_partner_id bigint NOT NULL,
    FOREIGN KEY (business_partner_id) REFERENCES business_partners (id)
);
CREATE INDEX idx_business_partners_name_parts_business_partner_id ON business_partners_name_parts USING btree (business_partner_id);

-- Create table for Roles entity
CREATE TABLE business_partners_roles
(
    role_name           VARCHAR(255) NOT NULL,
    business_partner_id bigint       NOT NULL,
    FOREIGN KEY (business_partner_id) REFERENCES business_partners (id)
);
CREATE INDEX idx_business_partners_roles_business_partner_id ON business_partners_roles USING btree (business_partner_id);

-- Create table for Identifier entity
CREATE TABLE business_partners_identifiers
(
    "value"             VARCHAR(255),
    type                VARCHAR(255),
    issuing_body        VARCHAR(255),
    business_partner_id bigint NOT NULL,
    FOREIGN KEY (business_partner_id) REFERENCES business_partners (id)
);
CREATE INDEX idx_business_partners_identifiers_business_partner_id ON business_partners_identifiers USING btree (business_partner_id);

-- Create table for State entity
CREATE TABLE business_partners_states
(
    description         VARCHAR(255),
    valid_from          timestamp(6) NULL,
    valid_to            timestamp(6) NULL,
    type                VARCHAR(255),
    business_partner_id bigint       NOT NULL,
    FOREIGN KEY (business_partner_id) REFERENCES business_partners (id)
);
CREATE INDEX idx_business_partners_states_business_partner_id ON business_partners_states USING btree (business_partner_id);

-- Create table for Classification entity
CREATE TABLE business_partners_classifications
(
    "value"             VARCHAR(255),
    code                VARCHAR(255),
    type                VARCHAR(255),
    business_partner_id bigint NOT NULL,
    FOREIGN KEY (business_partner_id) REFERENCES business_partners (id)
);
CREATE INDEX idx_business_partners_classifications_business_partner_id ON business_partners_classifications USING btree (business_partner_id);
