alter table logistic_addresses
    ADD phy_street_name_prefix varchar(255) NULL;
alter table logistic_addresses
    ADD phy_street_additional_name_prefix varchar(255) NULL;
alter table logistic_addresses
    ADD phy_street_name_suffix varchar(255) NULL;
alter table logistic_addresses
    ADD phy_street_additional_name_suffix varchar(255) NULL;