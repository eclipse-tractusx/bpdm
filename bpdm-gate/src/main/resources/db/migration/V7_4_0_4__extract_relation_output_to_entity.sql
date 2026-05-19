CREATE TABLE relation_outputs
(
    id                BIGINT                      NOT NULL,
    uuid              UUID                        NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    relation_type     VARCHAR(255)                NOT NULL,
    source_bpn        VARCHAR(255)                NOT NULL,
    target_bpn        VARCHAR(255)                NOT NULL,
    result_updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reason_code       VARCHAR(255),
    CONSTRAINT pk_relation_outputs PRIMARY KEY (id),
    CONSTRAINT uc_relation_outputs_uuid UNIQUE (uuid)
);

ALTER TABLE business_partner_relations
    ADD COLUMN output_id BIGINT;

ALTER TABLE relation_output_validity_periods
    ADD COLUMN output_id BIGINT;

DO
$$
    DECLARE
        r             RECORD;
        new_output_id BIGINT;
    BEGIN
        FOR r IN SELECT id,
                        output_relation_type,
                        output_source_bpn,
                        output_target_bpn,
                        output_updated_at,
                        output_reason_code
                 FROM business_partner_relations
                 WHERE output_relation_type IS NOT NULL
            LOOP
                new_output_id := nextval('bpdm_sequence');
                INSERT INTO relation_outputs (id, uuid, created_at, updated_at, relation_type, source_bpn, target_bpn,
                                             result_updated_at, reason_code)
                VALUES (new_output_id, gen_random_uuid(), NOW(), NOW(), r.output_relation_type, r.output_source_bpn,
                        r.output_target_bpn, r.output_updated_at, r.output_reason_code);

                UPDATE business_partner_relations SET output_id = new_output_id WHERE id = r.id;
                UPDATE relation_output_validity_periods SET output_id = new_output_id WHERE relation_id = r.id;
            END LOOP;
    END
$$;

ALTER TABLE relation_output_validity_periods
    DROP CONSTRAINT fk_output_validity_periods_relation;

DROP INDEX idx_output_validity_periods_relation_id;

ALTER TABLE relation_output_validity_periods
    DROP COLUMN relation_id;

ALTER TABLE relation_output_validity_periods
    ALTER COLUMN output_id SET NOT NULL;

ALTER TABLE relation_output_validity_periods
    ADD CONSTRAINT fk_output_validity_periods_relation FOREIGN KEY (output_id) REFERENCES relation_outputs (id);

CREATE INDEX idx_output_validity_periods_relation_id ON relation_output_validity_periods (output_id);

ALTER TABLE business_partner_relations
    ADD CONSTRAINT fk_relation_output FOREIGN KEY (output_id) REFERENCES relation_outputs (id);

ALTER TABLE business_partner_relations
    DROP COLUMN output_relation_type,
    DROP COLUMN output_source_bpn,
    DROP COLUMN output_target_bpn,
    DROP COLUMN output_updated_at,
    DROP COLUMN output_reason_code;
