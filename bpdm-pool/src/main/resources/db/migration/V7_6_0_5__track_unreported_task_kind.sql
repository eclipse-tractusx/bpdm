delete from golden_record_tasks;

alter table golden_record_tasks drop column is_resolved;
alter table golden_record_tasks drop column last_checked;
alter table golden_record_tasks add column task_kind varchar(255) not null;
