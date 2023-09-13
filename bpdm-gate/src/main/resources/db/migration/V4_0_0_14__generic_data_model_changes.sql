-- Make field mandatory
alter table business_partners
    alter column is_owner set not null;

-- Rename columns to bnpl/bpns/bpna for consistency
alter table business_partners
    rename column legal_entity_bpn to bpnl;
alter table business_partners
    rename column site_bpn to bpns;
alter table business_partners
    rename column address_bpn to bpna;
