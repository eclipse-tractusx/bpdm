-- Identifiers
alter table identifiers
    drop column status;

drop table identifier_status;

alter table identifiers
    drop column issuing_body_id;

drop table issuing_bodies;

alter table identifiers
    add issuing_body varchar(255);

alter table identifier_types
    drop column url;

-- LE names
alter table legal_entities
    add name_value varchar(255) not null default '';

alter table legal_entities
    add name_shortname varchar(255);

update legal_entities le
set name_value     = n.value,
    name_shortname = n.short_name
from names n
where n.legal_entity_id = le.id;

drop table names;

-- LE types
drop table legal_entity_types;

-- LE roles
drop table legal_entity_roles;

drop table roles;

-- LE bank accounts
drop table bank_account_trust_scores;

drop table bank_accounts;

-- Legal form
drop table legal_forms_legal_categories;

drop table legal_form_categories;

alter table legal_forms
    drop column url;

alter table legal_forms
    drop column language;

update legal_forms
set name = technical_key
where name is null;

alter table legal_forms
    alter column name set not null;

-- Classifications
delete
from classifications
where type is null;

alter table classifications
    alter column type set not null;

-- Relations
alter table relations
    drop column class;

alter table relations
    rename column started_at to valid_from;

alter table relations
    rename column ended_at to valid_to;

-- LE state
create table legal_entity_states
(
    id                  bigint                   not null
        constraint pk_legal_entity_states
            primary key,
    uuid                uuid                     not null
        constraint uc_legal_entity_states_uuid
            unique,
    created_at          timestamp with time zone not null,
    updated_at          timestamp with time zone not null,
    official_denotation varchar(255),
    valid_from          timestamp,
    valid_to            timestamp,
    type                varchar(255)             not null,
    legal_entity_id     bigint                   not null
        constraint fk_legal_entity_states_on_legal_entities
            references legal_entities (id)
);

create index idx_legal_entity_states_on_legal_entities
    on legal_entity_states (legal_entity_id);

alter table legal_entity_states
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table legal_entity_states
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';

-- Site state
create table site_states
(
    id          bigint                   not null
        constraint pk_site_states
            primary key,
    uuid        uuid                     not null
        constraint uc_site_states_uuid
            unique,
    created_at  timestamp with time zone not null,
    updated_at  timestamp with time zone not null,
    description varchar(255),
    valid_from  timestamp,
    valid_to    timestamp,
    type        varchar(255)             not null,
    site_id     bigint                   not null
        constraint fk_site_states_on_sites
            references sites (id)
);

create index idx_site_states_on_sites
    on site_states (site_id);

alter table site_states
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table site_states
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';

-- Old business stati
drop table business_stati;

-- Identifier types
alter table identifier_types
    add lsa_type varchar(255) default 'LEGAL_ENTITY' not null;

alter table country_identifier_types
    rename to identifier_type_details;

alter table identifier_types
    add constraint uc_identifier_types_technical_key_lsa_type unique (technical_key, lsa_type);

alter index country_identifier_types_pkey rename to identifier_type_details_pkey;

alter index uc_country_identifier_types_uuid rename to uc_identifier_type_details_uuid;

alter index uc_country_identifier_types_country_code_identifier_type_id rename to uc_identifier_type_details_country_code_identifier_type_id;

alter table identifier_type_details
    rename constraint fk_country_identifier_types_on_identifier_types to fk_identifier_type_details_on_identifier_types;

-- Legal entity identifiers
alter table identifiers
    rename to legal_entity_identifiers;

-- Region
create table regions
(
    id           bigint                   not null
        constraint pk_regions
            primary key,
    uuid         uuid                     not null
        constraint uc_regions_uuid
            unique,
    created_at   timestamp with time zone not null,
    updated_at   timestamp with time zone not null,
    country_code varchar(255)             not null,
    region_code  varchar(255)             not null,
    region_name  varchar(255)             not null,
    constraint uc_regions_region_code
        unique (region_code)
);

alter table regions
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table regions
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';

-- Logistic address
create table logistic_addresses
(
    id                          bigint                   not null
        constraint pk_logistic_addresses
            primary key,
    uuid                        uuid                     not null
        constraint uc_logistic_addresses_uuid
            unique,
    created_at                  timestamp with time zone not null,
    updated_at                  timestamp with time zone not null,
    bpn                         varchar(255)             not null
        constraint uc_logistic_addresses_bpn
            unique,
    name                        varchar(255),
    legal_entity_id             bigint
        constraint fk_logistic_addresses_on_legal_entity
            references legal_entities,
    site_id                     bigint
        constraint fk_logistic_addresses_on_site
            references sites,

    phy_latitude                double precision,
    phy_longitude               double precision,
    phy_altitude                double precision,
    phy_country                 varchar(255)             not null,
    phy_admin_area_l1_region    bigint
        constraint fk_logistic_addresses_on_phyregion
            references regions,
    phy_admin_area_l2           varchar(255),
    phy_admin_area_l3           varchar(255),
    phy_admin_area_l4           varchar(255),
    phy_postcode                varchar(255),
    phy_city                    varchar(255)             not null,
    phy_district_l1             varchar(255),
    phy_district_l2             varchar(255),
    phy_street_name             varchar(255),
    phy_street_number           varchar(255),
    phy_street_milestone        varchar(255),
    phy_street_direction        varchar(255),
    phy_industrial_zone         varchar(255),
    phy_building                varchar(255),
    phy_floor                   varchar(255),
    phy_door                    varchar(255),

    alt_latitude                double precision,
    alt_longitude               double precision,
    alt_altitude                double precision,
    alt_country                 varchar(255),
    alt_admin_area_l1_region    bigint
        constraint fk_logistic_addresses_on_altregion
            references regions,
    alt_admin_area_l2           varchar(255),
    alt_admin_area_l3           varchar(255),
    alt_admin_area_l4           varchar(255),
    alt_postcode                varchar(255),
    alt_city                    varchar(255),
    alt_district_l1             varchar(255),
    alt_district_l2             varchar(255),
    alt_street_name             varchar(255),
    alt_street_number           varchar(255),
    alt_street_milestone        varchar(255),
    alt_street_direction        varchar(255),
    alt_delivery_service_type   varchar(255),
    alt_delivery_service_number varchar(255)
);

alter table logistic_addresses
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table logistic_addresses
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';

-- Address state
create table address_states
(
    id          bigint                   not null
        constraint pk_address_states
            primary key,
    uuid        uuid                     not null
        constraint uc_address_states_uuid
            unique,
    created_at  timestamp with time zone not null,
    updated_at  timestamp with time zone not null,
    description varchar(255),
    valid_from  timestamp,
    valid_to    timestamp,
    type        varchar(255)             not null,
    address_id  bigint                   not null
        constraint fk_address_states_on_addresses
            references logistic_addresses
);

create index idx_address_states_on_addresses
    on address_states (address_id);

alter table address_states
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table address_states
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';

-- Address identifiers
create table address_identifiers
(
    id         bigint                   not null
        constraint pk_address_identifiers
            primary key,
    uuid       uuid                     not null
        constraint uc_address_identifiers_uuid
            unique,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    value      varchar(255)             not null,
    type_id    bigint                   not null
        constraint fk_address_identifiers_on_type
            references identifier_types,
    address_id bigint                   not null
        constraint fk_address_identifiers_on_address
            references logistic_addresses
);

create index idx_address_identifiers_on_type
    on address_identifiers (type_id);

create index idx_address_identifiers_on_address
    on address_identifiers (address_id);

alter table site_states
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table site_states
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';

-- Data is cleaned because of foreign key constraints
alter table legal_entities
    drop constraint fk_legal_entities_on_addresses;

alter table sites
    drop constraint fk_sites_on_addresses;

delete
from site_states;
delete
from sites;

delete
from legal_entity_identifiers;
delete
from legal_entity_states;
delete
from classifications;
delete
from relations;
delete
from legal_entities;

alter table legal_entities
    add constraint fk_legal_entities_on_legal_address
        foreign key (legal_address_id) references logistic_addresses;

alter table sites
    add constraint fk_sites_on_main_address
        foreign key (main_address_id) references logistic_addresses;

alter table sites
    rename constraint fk_sites_on_business_partners to fk_sites_on_legal_entity;

-- Cleanup
drop table care_ofs;
drop table address_contexts;
drop table address_types;
drop table administrative_areas;
drop table post_codes;
drop table thoroughfares;
drop table premises;
drop table postal_delivery_points;
drop table localities;
drop table address_partners;
drop table addresses;
