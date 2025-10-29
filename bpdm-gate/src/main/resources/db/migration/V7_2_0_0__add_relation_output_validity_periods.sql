CREATE TABLE relation_output_validity_periods (
    relation_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    CONSTRAINT fk_output_validity_periods_relation FOREIGN KEY (relation_id) REFERENCES business_partner_relations (id)
);

CREATE INDEX idx_output_validity_periods_relation_id ON relation_output_validity_periods  (relation_id);
