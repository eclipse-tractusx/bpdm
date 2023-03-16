CREATE SEQUENCE IF NOT EXISTS bpdm_gate_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE changelog
(
    id            BIGINT                      NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    businessPartner_type VARCHAR(255)         NOT NULL,
    CONSTRAINT pk_changelog PRIMARY KEY (id)
);

