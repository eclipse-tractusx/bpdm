create sequence bpdm_sequence start with 1 increment by 1;

create table business_partner_addresses (
  alt_exists boolean not null,
  alt_altitude float4,
  alt_latitude float4,
  alt_longitude float4,
  has_changed boolean,
  phy_altitude float4,
  phy_latitude float4,
  phy_longitude float4,
  task_id bigint not null,
  address_name varchar(255),
  alt_admin_area_l1_region varchar(255),
  alt_city varchar(255),
  alt_country varchar(255),
  alt_delivery_service_number varchar(255),
  alt_delivery_service_qualifier varchar(255),
  alt_delivery_service_type varchar(255) check (
    alt_delivery_service_type in ('PO_BOX', 'PRIVATE_BAG', 'BOITE_POSTALE')
  ),
  alt_postcode varchar(255),
  phy_admin_area_l1_region varchar(255),
  phy_admin_area_l2 varchar(255),
  phy_admin_area_l3 varchar(255),
  phy_building varchar(255),
  phy_city varchar(255),
  phy_company_postcode varchar(255),
  phy_country varchar(255),
  phy_direction varchar(255),
  phy_district_l1 varchar(255),
  phy_door varchar(255),
  phy_floor varchar(255),
  phy_house_number varchar(255),
  phy_house_number_supplement varchar(255),
  phy_industrial_zone varchar(255),
  phy_milestone varchar(255),
  phy_postcode varchar(255),
  phy_street_name varchar(255),
  phy_street_name_additional_prefix varchar(255),
  phy_street_name_additional_suffix varchar(255),
  phy_street_name_prefix varchar(255),
  phy_street_name_suffix varchar(255),
  phy_tax_jurisdiction varchar(255),
  scope varchar(255) not null check (
    scope in (
      'LegalAddress',
      'SiteMainAddress',
      'AdditionalAddress',
      'UncategorizedAddress'
    )
  ),
  constraint uc_business_partner_addresses_task_scope unique (task_id, scope)
);

create table business_partner_bpn_references (
  task_id bigint not null,
  desired_bpn varchar(255),
  scope varchar(255) not null check (
    scope in (
      'LegalEntity',
      'Site',
      'LegalAddress',
      'SiteMainAddress',
      'AdditionalAddress',
      'UncategorizedAddress'
    )
  ),
  type varchar(255) check (type in ('Bpn', 'BpnRequestIdentifier')),
  value varchar(255),
  constraint uc_business_partner_bpn_references_task_scope unique (task_id, scope)
);

create table business_partner_confidences (
  checked_by_external_datasource boolean,
  confidence_level integer,
  number_of_sharing_members integer,
  shared_by_owner boolean,
  last_confidence_check TIMESTAMP,
  next_confidence_check TIMESTAMP,
  task_id bigint not null,
  scope varchar(255) not null check (
    scope in (
      'LegalEntity',
      'Site',
      'Uncategorized',
      'LegalAddress',
      'SiteMainAddress',
      'AdditionalAddress',
      'UncategorizedAddress'
    )
  ),
  constraint uc_business_partner_confidences_task_scope unique (task_id, scope)
);

create table business_partner_identifiers (
  task_id bigint not null,
  index integer not null,
  issuing_body varchar(255),
  scope varchar(255) not null check (
    scope in (
      'LegalEntity',
      'Uncategorized',
      'LegalAddress',
      'SiteMainAddress',
      'AdditionalAddress',
      'UncategorizedAddress'
    )
  ),
  type varchar(255),
  value varchar(255)
);

create table business_partner_name_parts (
  index integer not null,
  task_id bigint not null,
  name varchar(255) not null,
  type varchar(255) check (
    type in (
      'LegalName',
      'ShortName',
      'LegalForm',
      'SiteName',
      'AddressName'
    )
  )
);

create table business_partner_states (
  task_id bigint not null,
  index integer not null,
  valid_from TIMESTAMP,
  valid_to TIMESTAMP,
  scope varchar(255) not null check (
    scope in (
      'LegalEntity',
      'Site',
      'Uncategorized',
      'LegalAddress',
      'SiteMainAddress',
      'AdditionalAddress',
      'UncategorizedAddress'
    )
  ),
  type varchar(255) check (type in ('ACTIVE', 'INACTIVE'))
);

create table golden_record_tasks (
  gate_record_id bigint not null,
  is_cx_member boolean,
  legal_entity_has_changed boolean,
  site_exists boolean not null,
  site_has_changed boolean,
  created_at TIMESTAMP not null,
  id bigint not null,
  task_pending_timeout TIMESTAMP,
  task_retention_timeout TIMESTAMP,
  updated_at TIMESTAMP not null,
  uuid uuid not null unique,
  legal_form varchar(255),
  legal_name varchar(255),
  legal_short_name varchar(255),
  owning_company_bpnl varchar(255),
  site_name varchar(255),
  task_mode varchar(255) not null check (
    task_mode in ('UpdateFromSharingMember', 'UpdateFromPool')
  ),
  task_result_state varchar(255) not null check (
    task_result_state in ('Pending', 'Success', 'Error')
  ),
  task_step varchar(255) not null check (task_step in ('CleanAndSync', 'PoolSync', 'Clean')),
  task_step_state varchar(255) not null check (
    task_step_state in ('Queued', 'Reserved', 'Success', 'Error')
  ),
  primary key (id)
);

create table task_errors (
  task_id bigint not null,
  description varchar(255) not null,
  type varchar(255) not null check (type in ('Timeout', 'Unspecified'))
);

create table gate_records (
    id bigint not null,
    created_at TIMESTAMP not null,
    updated_at TIMESTAMP not null,
    private_id uuid not null unique,
    public_id uuid not null unique,
    primary key(id)
);


create index index_tasks_uuid on golden_record_tasks (uuid);

create index index_tasks_step_step_state on golden_record_tasks (task_step, task_step_state);

create index index_tasks_pending_timeout on golden_record_tasks (task_pending_timeout);

create index index_tasks_retention_timeout on golden_record_tasks (task_retention_timeout);

alter table
  if exists golden_record_tasks
add
  constraint fk_tasks_gate_records foreign key (gate_record_id) references gate_records;

alter table
  if exists business_partner_addresses
add
  constraint fk_addresses_tasks foreign key (task_id) references golden_record_tasks;

alter table
  if exists business_partner_bpn_references
add
  constraint fk_bpn_references_tasks foreign key (task_id) references golden_record_tasks;

alter table
  if exists business_partner_confidences
add
  constraint fk_confidences_tasks foreign key (task_id) references golden_record_tasks;

alter table
  if exists business_partner_identifiers
add
  constraint fk_identifiers_tasks foreign key (task_id) references golden_record_tasks;

alter table
  if exists business_partner_name_parts
add
  constraint fk_name_parts_tasks foreign key (task_id) references golden_record_tasks;

alter table
  if exists business_partner_states
add
  constraint fk_states_tasks foreign key (task_id) references golden_record_tasks;

alter table
  if exists task_errors
add
  constraint fk_errors_tasks foreign key (task_id) references golden_record_tasks;
