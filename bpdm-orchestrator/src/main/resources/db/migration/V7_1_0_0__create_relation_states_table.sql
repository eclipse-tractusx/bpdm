-- Create table for storing relation states within a golden record task
CREATE TABLE relation_task_states (
    relation_id BIGINT NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    type VARCHAR(255) NOT NULL CHECK (
        type IN ('ACTIVE', 'INACTIVE')
    ),
    FOREIGN KEY (relation_id) REFERENCES relations_golden_record_tasks(id) ON DELETE CASCADE
);

-- Create index to improve lookup performance
CREATE INDEX idx_relation_task_states_relation_id ON relation_task_states (relation_id);
