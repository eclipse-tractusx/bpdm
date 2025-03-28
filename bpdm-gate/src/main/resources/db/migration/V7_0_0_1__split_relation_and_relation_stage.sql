CREATE TABLE business_partner_relation_stages
(
    id                      BIGINT                      NOT NULL,
    uuid                    UUID                        NOT NULL,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    relation_id             BIGINT                      NOT NULL    REFERENCES  business_partner_relations (id),
    relation_type           VARCHAR(255)                NOT NULL,
    stage                   VARCHAR(255)                NOT NULL,
    source_sharing_state_id BIGINT                      NOT NULL    REFERENCES  sharing_states (id),
    target_sharing_state_id BIGINT                      NOT NULL    REFERENCES  sharing_states (id),
    CONSTRAINT pk_business_partner_relation_stages PRIMARY KEY (id),
    CONSTRAINT uc_business_partner_relation_stages_relation_stage UNIQUE(relation_id, stage)
);

ALTER TABLE business_partner_relations
ADD COLUMN sharing_state_type VARCHAR(255) DEFAULT NULL,
ADD COLUMN sharing_error_code VARCHAR(255) DEFAULT NULL,
ADD COLUMN sharing_error_message VARCHAR(255) DEFAULT NULL,
ADD COLUMN sharing_state_updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL;

INSERT INTO business_partner_relation_stages (id, uuid, created_at, updated_at, relation_id, relation_type, stage, source_sharing_state_id, target_sharing_state_id)
SELECT nextval('bpdm_sequence'), gen_random_uuid(), NOW(), NOW(), id, relation_type, stage, source_sharing_state_id, target_sharing_state_id
FROM business_partner_relations;

ALTER TABLE business_partner_relations
DROP COLUMN stage,
DROP COLUMN relation_type,
DROP COLUMN source_sharing_state_id,
DROP COLUMN target_sharing_state_id;