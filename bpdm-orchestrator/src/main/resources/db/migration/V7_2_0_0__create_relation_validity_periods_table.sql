-- Create table for storing relation states within a golden record task
CREATE TABLE relation_task_validity_periods (
    relation_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    FOREIGN KEY (relation_id) REFERENCES relations_golden_record_tasks(id) ON DELETE CASCADE
);

-- Create index to improve lookup performance
CREATE INDEX idx_relation_task_validity_periods_relation_id ON relation_task_validity_periods (relation_id);
