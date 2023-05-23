-- field Rules
create table field_quality_rule
(
    id                 bigint                      not null,
    uuid               UUID                        not null,
    created_at         timestamp without time zone not null,
    updated_at         timestamp without time zone not null,
    country_code       varchar(255),
    field_path         varchar(255) not null,
    schema_name        varchar(255) not null,
    quality_level      varchar(255) not null,
    primary key (id)
);

alter table field_quality_rule
    add constraint uc_field_quality_rule_uuid unique (uuid);

alter table field_quality_rule
    add constraint uc_field_quality_rule_country_schema_field_id unique (country_code, field_path, schema_name);

commit;

