CREATE TABLE reason_codes
(
    id                          BIGINT                      NOT NULL,
    uuid                        UUID                        NOT NULL,
    created_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    technical_key               VARCHAR(255)                NoT NULL,
    description                 VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_reason_codes PRIMARY KEY (id),
    CONSTRAINT uc_reason_codes_technical_key UNIQUE (technical_key)
);

CREATE INDEX idx_reason_codes_technical_key ON reason_codes (technical_key);
