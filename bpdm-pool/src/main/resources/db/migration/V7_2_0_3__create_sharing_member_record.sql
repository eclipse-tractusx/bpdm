CREATE TABLE sharing_member_records
(
    id                          BIGINT                      NOT NULL,
    uuid                        UUID                        NOT NULL,
    created_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    record_id                   VARCHAR(255)                NOT NULL,
    is_golden_record_counted    BOOLEAN,
    address_id                  BIGINT                      NOT NULL,
    CONSTRAINT pk_address_versions PRIMARY KEY (id),
    CONSTRAINT fk_sharing_member_records_addresses FOREIGN KEY (address_id) REFERENCES logistic_addresses(id)
);

CREATE INDEX idx_sharing_member_records_record_id ON sharing_member_records (record_id);
CREATE INDEX idx_sharing_member_records_address_id ON sharing_member_records (address_id);
