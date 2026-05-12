CREATE TABLE business_partners_le_golden_record_relations
(
    business_partner_id BIGINT       NOT NULL,
    relation_type       VARCHAR(255) NOT NULL,
    source_bpn          VARCHAR(255) NOT NULL,
    target_bpn          VARCHAR(255) NOT NULL,
    CONSTRAINT fk_le_golden_record_relations_business_partners FOREIGN KEY (business_partner_id) REFERENCES business_partners (id)
);

CREATE INDEX index_le_golden_record_relations_bp_id ON business_partners_le_golden_record_relations (business_partner_id);

CREATE TABLE business_partners_address_golden_record_relations
(
    business_partner_id BIGINT       NOT NULL,
    relation_type       VARCHAR(255) NOT NULL,
    source_bpn          VARCHAR(255) NOT NULL,
    target_bpn          VARCHAR(255) NOT NULL,
    CONSTRAINT fk_address_golden_record_relations_business_partners FOREIGN KEY (business_partner_id) REFERENCES business_partners (id)
);

CREATE INDEX index_address_golden_record_relations_bp_id ON business_partners_address_golden_record_relations (business_partner_id);
