create table sites
(
    id         bigint       not null,
    bpn        varchar(255) not null,
    created_at timestamp    not null,
    updated_at timestamp    not null,
    uuid       uuid         not null,
    name       varchar(255),
    partner_id bigint       not null,
    primary key (id)
);

alter table sites
    add constraint uc_sites_bpn unique (bpn);

alter table sites
    add constraint uc_sites_uuid unique (uuid);

alter table sites
    add constraint fk_sites_on_business_partners foreign key (partner_id) references business_partners;

alter table addresses
    add site_id bigint;

alter table addresses
    add name varchar(255);

alter table addresses
    alter column partner_id drop not null;

alter table addresses
    add constraint fk_addresses_on_sites foreign key (site_id) references sites;

alter table addresses
    add bpn varchar(255) not null;

alter table addresses
    add constraint uc_addresses_bpn unique (bpn);

update configuration_entries
set key='bpn-l-counter'
where key = 'bpn-counter'
