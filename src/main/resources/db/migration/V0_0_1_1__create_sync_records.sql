CREATE TABLE sync_records
(
    id             BIGINT                      NOT NULL,
    uuid           UUID                        NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    type           VARCHAR(255)                NOT NULL,
    status         VARCHAR(255)                NOT NULL,
    progress       FLOAT                       NOT NULL,
    status_details VARCHAR(255),
    save_state     VARCHAR(255),
    started_at     TIMESTAMP with time zone,
    finished_at    TIMESTAMP with time zone,
    CONSTRAINT pk_sync_records PRIMARY KEY (id)
);

ALTER TABLE sync_records
    ADD CONSTRAINT uc_sync_records_type UNIQUE (type);

ALTER TABLE sync_records
    ADD CONSTRAINT uc_sync_records_uuid UNIQUE (uuid);