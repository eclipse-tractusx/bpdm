-- Create address_relations table
CREATE TABLE address_relations (
    id BIGINT NOT NULL DEFAULT nextval('bpdm_sequence'),
    UUID UUID NOT NULL,
    type VARCHAR(255) NOT NULL CHECK (type IN ('IsReplacedBy')),
    start_address_id BIGINT NOT NULL,
    end_address_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

-- Indexes for address_relations
CREATE INDEX idx_address_relations_start_address ON address_relations (start_address_id);
CREATE INDEX idx_address_relations_end_address ON address_relations (end_address_id);

-- FK to logistic_addresses
ALTER TABLE address_relations
    ADD CONSTRAINT fk_address_rel_start
    FOREIGN KEY (start_address_id)
    REFERENCES logistic_addresses (id)
    ON DELETE CASCADE;

ALTER TABLE address_relations
    ADD CONSTRAINT fk_address_rel_end
    FOREIGN KEY (end_address_id)
    REFERENCES logistic_addresses (id)
    ON DELETE CASCADE;

-- Create address_relation_validity_periods table (element collection for address relations)
CREATE TABLE address_relation_validity_periods (
    id BIGINT NOT NULL DEFAULT nextval('bpdm_sequence'),
    relation_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    PRIMARY KEY (id)
);

-- Index for validity periods
CREATE INDEX idx_address_relation_validity_periods_relation_id ON address_relation_validity_periods (relation_id);

-- FK from validity periods to address_relations
ALTER TABLE address_relation_validity_periods
    ADD CONSTRAINT  fk_address_relation_validity_periods_relation
    FOREIGN KEY (relation_id)
    REFERENCES address_relations (id)
    ON DELETE CASCADE;