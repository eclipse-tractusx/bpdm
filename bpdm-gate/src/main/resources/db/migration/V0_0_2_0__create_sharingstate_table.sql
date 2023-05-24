
CREATE TABLE sharing_states
(
    id            BIGINT                      NOT NULL,
    uuid          UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    external_id  VARCHAR(255)        NOT NULL,
    lsa_type VARCHAR(255)        NOT NULL,
    sharing_state_type VARCHAR(255)        NOT NULL,
    sharing_error_code VARCHAR(255),
    sharing_error_message VARCHAR(255),
    bpn VARCHAR(255),
    sharing_process_started timestamp with time zone,
    CONSTRAINT pk_sharing_states PRIMARY KEY (id)
);

ALTER TABLE sharing_states
    ADD CONSTRAINT uc_sharing_states_uuid UNIQUE (uuid);
ALTER TABLE sharing_states
    ADD CONSTRAINT uc_sharing_states_externalId_lsa_type UNIQUE (external_id, lsa_type);

alter table sharing_states
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table sharing_states
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
 alter table sharing_states
     alter column sharing_process_started type timestamp with time zone using sharing_process_started at time zone 'UTC';
