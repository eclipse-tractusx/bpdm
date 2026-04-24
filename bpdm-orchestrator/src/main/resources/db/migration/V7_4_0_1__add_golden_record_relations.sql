CREATE TABLE business_partner_le_golden_record_relations
(
    task_id       BIGINT       NOT NULL,
    relation_type VARCHAR(255) NOT NULL,
    source_bpn    VARCHAR(255) NOT NULL,
    target_bpn    VARCHAR(255) NOT NULL,
    CONSTRAINT fk_le_golden_record_relations_tasks FOREIGN KEY (task_id) REFERENCES golden_record_tasks (id)
);

CREATE INDEX index_le_golden_record_relations_task_id ON business_partner_le_golden_record_relations (task_id);

CREATE TABLE business_partner_address_golden_record_relations
(
    task_id       BIGINT       NOT NULL,
    relation_type VARCHAR(255) NOT NULL,
    source_bpn    VARCHAR(255) NOT NULL,
    target_bpn    VARCHAR(255) NOT NULL,
    scope         VARCHAR(255) NOT NULL,
    CONSTRAINT fk_address_golden_record_relations_tasks FOREIGN KEY (task_id) REFERENCES golden_record_tasks (id)
);

CREATE INDEX index_address_golden_record_relations_task_id ON business_partner_address_golden_record_relations (task_id);
