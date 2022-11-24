create table country_identifier_types
(
    id                 bigint                      not null,
    created_at         timestamp without time zone not null,
    updated_at         timestamp without time zone not null,
    uuid               uuid                        not null,
    country_code       varchar(255),
    mandatory          boolean                     not null,
    identifier_type_id bigint                      not null,
    primary key (id)
);

alter table country_identifier_types
    add constraint uc_country_identifier_types_uuid unique (uuid);

alter table country_identifier_types
    add constraint fk_country_identifier_types_on_identifier_types foreign key (identifier_type_id) references identifier_types;

alter table country_identifier_types
    add constraint uc_country_identifier_types_country_code_identifier_type_id unique (country_code, identifier_type_id);
