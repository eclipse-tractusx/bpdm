alter table sync_records
    add column from_time timestamp without time zone not null default '1970-01-01 08:00:00';