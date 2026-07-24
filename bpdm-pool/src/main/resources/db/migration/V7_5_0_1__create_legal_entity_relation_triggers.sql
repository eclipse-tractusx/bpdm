CREATE TABLE legal_entity_relation_event_triggers
(
    id           BIGINT                      NOT NULL,
    uuid         UUID                        NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    trigger_date DATE                        NOT NULL,
    is_processed BOOLEAN                     NOT NULL,
    event_type   VARCHAR(255)                NOT NULL,
    relation_id  BIGINT                      NOT NULL,
    CONSTRAINT pk_legal_entity_relation_event_triggers PRIMARY KEY (id),
    CONSTRAINT uc_le_relation_event_type_trigger_date UNIQUE (relation_id, event_type, trigger_date)
);

CREATE INDEX idx_le_relation_event_triggers_relation_event_type ON legal_entity_relation_event_triggers (relation_id, event_type);
CREATE INDEX idx_le_relation_event_triggers_is_processed_trigger_date ON legal_entity_relation_event_triggers (is_processed, trigger_date);

