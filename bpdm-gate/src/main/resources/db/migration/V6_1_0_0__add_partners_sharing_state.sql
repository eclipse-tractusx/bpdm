ALTER TABLE business_partners
ADD COLUMN sharing_state_id  BIGINT DEFAULT NULL;

ALTER TABLE business_partners
ADD CONSTRAINT fk_business_partners_sharing_state FOREIGN KEY (sharing_state_id) REFERENCES sharing_states (id)
