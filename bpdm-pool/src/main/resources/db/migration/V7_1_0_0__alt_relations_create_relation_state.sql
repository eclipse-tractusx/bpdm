-- Remove the is_active column
ALTER TABLE relations DROP COLUMN IF EXISTS is_active;

-- Create relation_states embeddable collection
CREATE TABLE relation_states (
    relation_id BIGINT NOT NULL,
    valid_from TIMESTAMP NOT NULL DEFAULT '1970-01-01T00:00:00Z',
    valid_to TIMESTAMP NOT NULL DEFAULT '9999-12-31T23:59:59Z',
    type VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_states_relation
        FOREIGN KEY (relation_id)
        REFERENCES relations(id)
        ON DELETE CASCADE
);

-- Optional index for performance
CREATE INDEX idx_relation_states_relation_id ON relation_states(relation_id);

