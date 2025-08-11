-- Remove the is_active column
ALTER TABLE relations DROP COLUMN IF EXISTS is_active;

-- Create relation_states embeddable collection
CREATE TABLE relation_states (
    relation_id BIGINT NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    type VARCHAR(255) NOT NULL CHECK (type IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_states_relation
        FOREIGN KEY (relation_id)
        REFERENCES relations(id)
        ON DELETE CASCADE
);

-- Optional index for performance
CREATE INDEX idx_relation_states_relation_id ON relation_states(relation_id);

