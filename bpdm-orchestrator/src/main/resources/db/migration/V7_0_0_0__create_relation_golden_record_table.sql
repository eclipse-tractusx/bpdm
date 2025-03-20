-- Create table relations_golden_record_tasks
CREATE TABLE relations_golden_record_tasks (
    id BIGINT NOT NULL DEFAULT nextval('bpdm_sequence'),
    uuid UUID NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    gate_record_id BIGINT NOT NULL,
    task_mode VARCHAR(255) NOT NULL CHECK (
        task_mode IN ('UpdateFromSharingMember', 'UpdateFromPool')
    ),
    task_result_state VARCHAR(255) NOT NULL CHECK (
        task_result_state IN ('Pending', 'Success', 'Error', 'Aborted')
    ),
    task_step VARCHAR(255) NOT NULL CHECK (
        task_step IN ('CleanAndSync', 'PoolSync', 'Clean')
    ),
    task_step_state VARCHAR(255) NOT NULL CHECK (
        task_step_state IN ('Queued', 'Reserved', 'Success', 'Error', 'Aborted')
    ),
    task_pending_timeout TIMESTAMP,
    task_retention_timeout TIMESTAMP,
    relation_type VARCHAR(255) NOT NULL,
    source_bpnl VARCHAR(255) NOT NULL,
    target_bpnl VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

-- Create relations_task_errors table for relations_golden_record_tasks
CREATE TABLE relations_task_errors (
    task_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL CHECK (
        type IN ('Timeout', 'Unspecified')
    ),
    FOREIGN KEY (task_id) REFERENCES relations_golden_record_tasks(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX index_relations_tasks_uuid ON relations_golden_record_tasks (uuid);
CREATE INDEX index_relations_tasks_step_step_state ON relations_golden_record_tasks (task_step, task_step_state);
CREATE INDEX index_relations_tasks_pending_timeout ON relations_golden_record_tasks (task_pending_timeout);
CREATE INDEX index_relations_tasks_retention_timeout ON relations_golden_record_tasks (task_retention_timeout);
CREATE INDEX index_relations_tasks_result_state_and_updated_at ON relations_golden_record_tasks (task_result_state, updated_at);
CREATE INDEX index_relations_task_errors_task_id ON relations_task_errors (task_id);

-- Foreign key constraints
ALTER TABLE relations_golden_record_tasks ADD CONSTRAINT fk_tasks_gate_records FOREIGN KEY (gate_record_id) REFERENCES gate_records(id) ON DELETE CASCADE;
