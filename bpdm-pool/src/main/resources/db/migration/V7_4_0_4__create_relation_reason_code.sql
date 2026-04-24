ALTER TABLE relations
ADD COLUMN reason_code_id BIGINT NOT NULL,
ADD CONSTRAINT fk_relations_reason_code_id FOREIGN KEY (reason_code_id) REFERENCES reason_codes(id);

ALTER TABLE address_relations
ADD COLUMN reason_code_id BIGINT NOT NULL,
ADD CONSTRAINT fk_address_relations_reason_code_id FOREIGN KEY (reason_code_id) REFERENCES reason_codes(id);
