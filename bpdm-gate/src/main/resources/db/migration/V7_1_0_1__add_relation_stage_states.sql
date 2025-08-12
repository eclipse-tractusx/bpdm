CREATE TABLE relation_stage_states (
    relation_stage_id BIGINT NOT NULL,
    valid_from TIMESTAMP NOT NULL DEFAULT '1970-01-01T00:00:00Z',
    valid_to TIMESTAMP NOT NULL DEFAULT '9999-12-31T23:59:59Z',
    state_type VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_stage_states_relation FOREIGN KEY (relation_stage_id) REFERENCES business_partner_relation_stages (id)
);

CREATE INDEX idx_stage_states_relation_id ON relation_stage_states (relation_stage_id);
