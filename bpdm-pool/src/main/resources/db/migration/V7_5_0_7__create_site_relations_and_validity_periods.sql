-- Create site_relations table
CREATE TABLE site_relations (
    id BIGINT NOT NULL DEFAULT nextval('bpdm_sequence'),
    UUID UUID NOT NULL,
    type VARCHAR(255) NOT NULL CHECK (type IN ('IsReplacedBy')),
    start_site_id BIGINT NOT NULL,
    end_site_id BIGINT NOT NULL,
    reason_code_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

-- Indexes for site_relations
CREATE INDEX idx_site_relations_start_site ON site_relations (start_site_id);
CREATE INDEX idx_site_relations_end_site ON site_relations (end_site_id);

-- FKs to sites
ALTER TABLE site_relations
    ADD CONSTRAINT fk_site_rel_start
    FOREIGN KEY (start_site_id)
    REFERENCES sites (id)
    ON DELETE CASCADE;

ALTER TABLE site_relations
    ADD CONSTRAINT fk_site_rel_end
    FOREIGN KEY (end_site_id)
    REFERENCES sites (id)
    ON DELETE CASCADE;

-- FK to reason_codes
ALTER TABLE site_relations
    ADD CONSTRAINT fk_site_relations_reason_code_id
    FOREIGN KEY (reason_code_id)
    REFERENCES reason_codes (id);

-- Create site_relation_validity_periods table (element collection for site relations)
CREATE TABLE site_relation_validity_periods (
    id BIGINT NOT NULL DEFAULT nextval('bpdm_sequence'),
    relation_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    PRIMARY KEY (id)
);

-- Index for validity periods
CREATE INDEX idx_site_relation_validity_periods_relation_id ON site_relation_validity_periods (relation_id);

-- FK from validity periods to site_relations
ALTER TABLE site_relation_validity_periods
    ADD CONSTRAINT fk_site_relation_validity_periods_relation
    FOREIGN KEY (relation_id)
    REFERENCES site_relations (id)
    ON DELETE CASCADE;
