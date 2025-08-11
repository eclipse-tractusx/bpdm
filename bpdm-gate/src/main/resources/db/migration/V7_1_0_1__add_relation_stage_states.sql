CREATE TABLE relation_stage_states (
    relation_stage_id BIGINT NOT NULL,
    valid_from TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    state_type VARCHAR(255) NOT NULL,
    CONSTRAINT fk_stage_states_relation FOREIGN KEY (relation_stage_id) REFERENCES business_partner_relation_stages (id)
);

CREATE INDEX idx_stage_states_relation_id ON relation_stage_states (relation_stage_id);
