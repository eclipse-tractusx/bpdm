alter table sync_records
alter
column started_at type timestamp without time zone using started_at::timestamp without time zone;

alter table sync_records
alter
column finished_at type timestamp without time zone using finished_at::timestamp without time zone;

