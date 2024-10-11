create index index_task_errors_task_id on task_errors (task_id);

create index index_name_parts_task_id on business_partner_name_parts (task_id);

create index index_identifiers_task_id on business_partner_identifiers (task_id);

create index index_business_states_task_id on business_partner_states (task_id);

create index index_confidences_task_id on business_partner_confidences (task_id);

create index index_addresses_task_id on business_partner_addresses (task_id);

create index index_bpn_references_task_id on business_partner_bpn_references (task_id);
