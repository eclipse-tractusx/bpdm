CREATE TABLE relation_output_states (
    relation_id BIGINT NOT NULL,
    valid_from TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    state_type VARCHAR(255) NOT NULL,
    CONSTRAINT fk_output_states_relation FOREIGN KEY (relation_id) REFERENCES business_partner_relations (id)
);

CREATE INDEX idx_output_states_relation_id ON relation_output_states (relation_id);
