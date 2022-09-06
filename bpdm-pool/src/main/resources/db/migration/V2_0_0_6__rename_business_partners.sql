alter table business_partners
    rename constraint pk_business_partners to pk_legal_entities;
alter table business_partners
    rename constraint uc_business_partners_bpn to uc_legal_entities_bpn;
alter table business_partners
    rename constraint uc_business_partners_uuid to uc_legal_entities_uuid;
alter table business_partners
    rename constraint fk_business_partners_on_addresses to fk_legal_entities_on_addresses;
alter table business_partners
    rename constraint fk_business_partners_on_legal_form to fk_legal_entities_on_legal_form;

alter table business_partners_roles
    rename constraint pk_business_partners_roles to pk_legal_entities_roles;

alter index idx_business_partner_types rename to idx_legal_entity_types;

alter table business_partner_types
    rename column partner_id to legal_entity_id;

alter table business_partners_roles
    rename column partner_id to legal_entity_id;

alter table identifiers
    rename column partner_id to legal_entity_id;

alter table names
    rename column partner_id to legal_entity_id;

alter table business_stati
    rename column partner_id to legal_entity_id;

alter table sites
    rename column partner_id to legal_entity_id;

alter table classifications
    rename column partner_id to legal_entity_id;

alter table bank_accounts
    rename column partner_id to legal_entity_id;


alter table business_partners
    rename to legal_entities;
alter table business_partners_roles
    rename to legal_entity_roles;
alter table business_partner_types
    rename to legal_entity_types;

