CREATE TABLE address_identifiers (
	id int8 NOT NULL,
	uuid uuid NOT NULL,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
	value varchar(255) NOT NULL,
	identifier_type_key varchar(255) NOT NULL,
	address_id int8 NOT NULL,
	CONSTRAINT pk_address_identifiers PRIMARY KEY (id),
	CONSTRAINT uc_address_identifiers_uuid UNIQUE (uuid)
);
CREATE INDEX idx_address_identifiers_on_address ON address_identifiers USING btree (address_id);


ALTER TABLE address_identifiers ADD CONSTRAINT fk_address_identifiers_on_address FOREIGN KEY (address_id) REFERENCES logistic_addresses(id);