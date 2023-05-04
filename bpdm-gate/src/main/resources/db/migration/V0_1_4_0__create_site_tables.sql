create table site_states (
  id bigint not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  uuid uuid not null,
  description varchar(255),
  type varchar(255) not null,
  valid_from timestamp,
  valid_to timestamp,
  site_id bigint not null,
  primary key (id)
);

create table sites (
  id bigint not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  uuid uuid not null,
  bpn varchar(255) null,
  external_id varchar(255) not null,
  name varchar(255) not null,
  primary key (id)
);

create index IDX4wafjs5sojc79luhn5l29bb79 on site_states (site_id);

alter table if exists site_states add constraint UK_qr49l0kw056r00i88edmvjwqa unique (uuid);

alter table if exists sites add constraint UK_b2t72lxjqja93ids61sgvo4hg unique (uuid);

alter table if exists sites add constraint UK_1vrdeiex4x7p93r5svtvb5b4x unique (external_id);

alter table if exists site_states add constraint FK7t400j8drx0gxk0davixv7n54 foreign key (site_id) references sites;

