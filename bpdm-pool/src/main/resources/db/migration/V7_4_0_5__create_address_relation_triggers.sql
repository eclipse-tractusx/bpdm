CREATE TABLE address_relation_event_triggers
(
    id                          BIGINT                      NOT NULL,
    uuid                        UUID                        NOT NULL,
    created_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    trigger_date                DATE                        NOT NULL,
    is_processed                BOOLEAN                     NOT NULL,
    event_type                  VARCHAR(255)                NOT NULL,
    relation_id                 BIGINT                      NOT NULL,
    CONSTRAINT pk_address_relation_event_triggers PRIMARY KEY (id),
    CONSTRAINT uc_address_relation_event_type_trigger_date UNIQUE (trigger_date, event_type, relation_id)
);

CREATE INDEX idx_address_relation_event_triggers_relation_event_type ON address_relation_event_triggers (relation_id, event_type);
CREATE INDEX idx_address_relation_event_triggers_is_processed_trigger_date ON address_relation_event_triggers (is_processed, trigger_date);


