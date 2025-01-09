DELETE
FROM bpn_requestidentifier_mapping dup_mapping
USING bpn_requestidentifier_mapping dist_mapping
WHERE dup_mapping.id < dist_mapping.id
AND dup_mapping.request_identifier = dist_mapping.request_identifier;

alter table bpn_requestidentifier_mapping
    drop constraint uc_field_request_identifier;

alter table bpn_requestidentifier_mapping
    add constraint uc_field_request_identifier unique(request_identifier);
