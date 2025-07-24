ALTER TABLE relations DROP COLUMN IF EXISTS is_active;

CREATE TABLE IF NOT EXISTS relation_states (
    relation_id BIGINT NOT NULL REFERENCES relations(id) ON DELETE CASCADE,
    index INTEGER NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    type VARCHAR(255) NOT NULL check (type in ('ACTIVE', 'INACTIVE')),
    PRIMARY KEY (relation_id, index)
);

CREATE INDEX IF NOT EXISTS idx_relation_states_relation_id ON relation_states(relation_id);
