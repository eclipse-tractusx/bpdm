-- Remove the is_active column
ALTER TABLE relations DROP COLUMN IF EXISTS is_active;

-- Create relation_states embeddable collection
CREATE TABLE relation_validity_periods (
    relation_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    CONSTRAINT fk_validity_periods_relation
        FOREIGN KEY (relation_id)
        REFERENCES relations(id)
        ON DELETE CASCADE
);

-- Optional index for performance
CREATE INDEX idx_relation_validity_periods_relation_id ON relation_validity_periods(relation_id);

