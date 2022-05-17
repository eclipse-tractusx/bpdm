alter table business_partners
    add currentness timestamp without time zone;

update business_partners
set currentness = created_at
where currentness is null;

alter table business_partners
    alter column currentness set not null;
