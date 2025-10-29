CREATE TABLE relation_stage_validity_periods  (
    relation_stage_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    CONSTRAINT fk_stage_validity_periods_relation FOREIGN KEY (relation_stage_id) REFERENCES business_partner_relation_stages (id)
);

CREATE INDEX idx_stage_validity_periods_relation_id ON relation_stage_validity_periods (relation_stage_id);
