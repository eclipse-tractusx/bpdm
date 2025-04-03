CREATE TABLE business_partners_output_relations
(
    business_partner_id     BIGINT                      NOT NULL    REFERENCES  business_partners (id),
    output_relation_type    VARCHAR(255)                NOT NULL,
    output_source_bpnl      VARCHAR(255)                NOT NULL,
    output_target_bpnl      VARCHAR(255)                NOT NULL,
    output_updated_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uc_business_partner_relations_id_type_source_target UNIQUE(business_partner_id, output_relation_type, output_source_bpnl, output_target_bpnl)
);