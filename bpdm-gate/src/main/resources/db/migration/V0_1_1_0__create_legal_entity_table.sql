
CREATE TABLE bpdmgate.legal_entities (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	bpn varchar(255) NOT NULL,
	currentness timestamp with time zone NOT NULL,
	external_id varchar(255) NOT NULL,
	legal_form_id varchar(255) NOT NULL,
	name_shortname varchar(255) NULL,
	name_value varchar(255) NOT NULL,
	CONSTRAINT legal_entities_pkey PRIMARY KEY (id),
	CONSTRAINT uk_fn3cbtgn4mcc8qprvf6nc33rb UNIQUE (external_id),
	CONSTRAINT uk_kr9fdq30p6pldlbqktqd6g58c UNIQUE (uuid)

);
CREATE INDEX idxm9ojfna20safop6xndvj1510n ON bpdmgate.legal_entities USING btree (legal_form_id);


CREATE TABLE bpdmgate.classifications (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	code varchar(255) NULL,
	"type" varchar(255) NULL,
	value varchar(255) NULL,
	legal_entity_id int8 NOT NULL,
	CONSTRAINT classifications_pkey PRIMARY KEY (id),
	CONSTRAINT uk_ssmeq1nlxrx3yyk9fg8by9kac UNIQUE (uuid),
	CONSTRAINT fk5b0t7pv9g32ifp4kew3ubxbft FOREIGN KEY (legal_entity_id) REFERENCES bpdmgate.legal_entities(id)
);
CREATE INDEX idx6g01nxco0d10y4an0kt91rwp6 ON bpdmgate.classifications USING btree (legal_entity_id);



CREATE TABLE bpdmgate.legal_entity_identifiers (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	issuing_body varchar(255) NULL,
	identifier_type_key varchar(255) NULL,
	value varchar(255) NOT NULL,
	legal_entity_id int8 NOT NULL,
	CONSTRAINT legal_entity_identifiers_pkey PRIMARY KEY (id),
	CONSTRAINT uk_imqmfmhu6wrvreenvgqlayymi UNIQUE (uuid),
	CONSTRAINT fk5jhxbd4f61s9u3uvjyuq6xmw9 FOREIGN KEY (legal_entity_id) REFERENCES bpdmgate.legal_entities(id)
);
CREATE INDEX idxit4n9hoeyov4gg4x8xn0g7tg1 ON bpdmgate.legal_entity_identifiers USING btree (legal_entity_id);



CREATE TABLE bpdmgate.legal_entity_states (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	official_denotation varchar(255) NULL,
	"type" varchar(255) NOT NULL,
	valid_from timestamp(6) NULL,
	valid_to timestamp(6) NULL,
	legal_entity_id int8 NOT NULL,
	CONSTRAINT legal_entity_states_pkey PRIMARY KEY (id),
	CONSTRAINT uk_nlgy0iv2uhkhqnmqxddx3j1dm UNIQUE (uuid),
	CONSTRAINT fkaq90sm3jrkxcuvjpliok8xh8x FOREIGN KEY (legal_entity_id) REFERENCES bpdmgate.legal_entities(id)
);
CREATE INDEX idxsy1y2c4dn86xrq2dy8assdmgg ON bpdmgate.legal_entity_states USING btree (legal_entity_id);



CREATE TABLE bpdmgate.relations (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"type" varchar(255) NOT NULL,
	valid_from timestamp(6) NULL,
	valid_to timestamp(6) NULL,
	end_node_id int8 NOT NULL,
	start_node_id int8 NOT NULL,
	CONSTRAINT relations_pkey PRIMARY KEY (id),
	CONSTRAINT uk_qylrhr8gciihnu82gvhwywwny UNIQUE (uuid),
	CONSTRAINT fk2ouq8tg0bvejr2o5oro5t4fav FOREIGN KEY (end_node_id) REFERENCES bpdmgate.legal_entities(id),
	CONSTRAINT fkej2415xh8whdtt0lvow1qbdur FOREIGN KEY (start_node_id) REFERENCES bpdmgate.legal_entities(id)
);
CREATE INDEX idx9kgukliy4u1s9woyd0tk1nmm3 ON bpdmgate.relations USING btree (start_node_id);
CREATE INDEX idxqhf4eyra7qpla4kh89miwsnv1 ON bpdmgate.relations USING btree (end_node_id);
