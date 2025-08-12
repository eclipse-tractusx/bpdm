CREATE TABLE relation_output_states (
    relation_id BIGINT NOT NULL,
    valid_from TIMESTAMP NOT NULL DEFAULT '1970-01-01T00:00:00Z',
    valid_to TIMESTAMP NOT NULL DEFAULT '9999-12-31T23:59:59Z',
    state_type VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_output_states_relation FOREIGN KEY (relation_id) REFERENCES business_partner_relations (id)
);

CREATE INDEX idx_output_states_relation_id ON relation_output_states (relation_id);
