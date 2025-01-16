CREATE TABLE business_partner_relations
(
    id                      BIGINT                      NOT NULL,
    uuid                    UUID                        NOT NULL,
    external_id             VARCHAR(255)                NOT NULL,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    relation_type           VARCHAR(255)                NOT NULL,
    stage                   VARCHAR(255)                NOT NULL,
    tenant_bpnl             VARCHAR(255)                NOT NULL,
    source_sharing_state_id BIGINT                      NOT NULL    REFERENCES  sharing_states (id),
    target_sharing_state_id BIGINT                      NOT NULL    REFERENCES  sharing_states (id),
    CONSTRAINT pk_business_partner_relationships PRIMARY KEY (id),
    UNIQUE(source_sharing_state_id, target_sharing_state_id),
    UNIQUE(external_id, stage, tenant_bpnl)
);