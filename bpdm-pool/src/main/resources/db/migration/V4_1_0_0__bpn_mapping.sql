-- field Rules
create table bpn_requestidentifier_mapping
(
    id                 bigint                      not null,
    uuid               UUID                        not null,
    created_at         timestamp without time zone not null,
    updated_at         timestamp without time zone not null,
    request_identifier       varchar(255) not null,
    bpn       varchar(255) not null,
    primary key (id)
);

alter table bpn_requestidentifier_mapping
    add constraint uc_field_request_identifier unique (uuid);

create index idx_request_identifier_on_bpn_requestidentifier_mapping
    on bpn_requestidentifier_mapping (request_identifier);

commit;

