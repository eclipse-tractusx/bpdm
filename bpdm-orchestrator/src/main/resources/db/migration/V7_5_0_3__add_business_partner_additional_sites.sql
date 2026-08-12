CREATE TABLE business_partner_additional_sites
(
    task_id     BIGINT NOT NULL,
    value       VARCHAR(255),
    desired_bpn VARCHAR(255),
    type        VARCHAR(255) CHECK (type IN ('Bpn', 'BpnRequestIdentifier')),
    site_name   VARCHAR(255),
    CONSTRAINT fk_additional_sites_tasks FOREIGN KEY (task_id) REFERENCES golden_record_tasks (id)
);

CREATE INDEX index_additional_sites_task_id ON business_partner_additional_sites (task_id);
