CREATE TABLE legal_entity_identifiers (
	id int8 NOT NULL,
	uuid uuid NOT NULL,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
	value varchar(255) NOT NULL,
	type_id varchar(255) NOT NULL,
	legal_entity_id int8 NOT NULL,
	issuing_body varchar(255) NULL,
	CONSTRAINT pk_identifiers PRIMARY KEY (id),
	CONSTRAINT uc_identifiers_uuid UNIQUE (uuid)
);
CREATE INDEX idx_9de08b456309ac30a77546592 ON legal_entity_identifiers USING btree (legal_entity_id);

ALTER TABLE legal_entity_identifiers ADD CONSTRAINT fk_identifiers_on_partner FOREIGN KEY (legal_entity_id) REFERENCES legal_entities(id);