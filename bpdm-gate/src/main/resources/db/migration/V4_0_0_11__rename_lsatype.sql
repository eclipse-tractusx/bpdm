alter table sharing_states
    rename column lsa_type to business_partner_type;

alter index uc_sharing_states_externalid_lsa_type rename to uc_sharing_states_externalid_business_partner_type;
