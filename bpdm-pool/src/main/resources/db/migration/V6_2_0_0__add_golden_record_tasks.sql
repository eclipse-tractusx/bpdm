create table golden_record_tasks
(
    id                 bigint                      not null,
    uuid               UUID                        not null,
    created_at         timestamp without time zone not null,
    updated_at         timestamp without time zone not null,
    task_id            varchar(255) not null,
    is_resolved      boolean not null,
    last_checked       timestamp without time zone not null,
    primary key (id)
);