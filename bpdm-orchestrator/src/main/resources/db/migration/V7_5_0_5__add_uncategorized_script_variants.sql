CREATE TABLE business_partner_uncategorized_script_variants
(
    task_id     BIGINT       NOT NULL,
    index       INTEGER      NOT NULL,
    script_code VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    CONSTRAINT fk_uncategorized_script_variants_tasks FOREIGN KEY (task_id) REFERENCES golden_record_tasks (id)
);

CREATE INDEX index_uncategorized_script_variants_task_id ON business_partner_uncategorized_script_variants (task_id);
