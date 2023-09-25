alter table bpdmgate.postal_addresses
    alter column phy_country drop not null;

alter table bpdmgate.postal_addresses
    alter column phy_city drop not null;
