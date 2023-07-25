alter table identifier_types
    rename column lsa_type to business_partner_type;

alter table identifier_types
    rename constraint uc_identifier_types_technical_key_lsa_type to uc_identifier_types_technical_key_business_partner_type;
