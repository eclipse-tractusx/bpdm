
CREATE TABLE "bpdm-gate".addresses (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	bpn varchar(255) NULL,
	care_of varchar(255) NULL,
	country varchar(255) NOT NULL,
	external_id varchar(255) NOT NULL,
	altitude float4 NULL,
	latitude float4 NULL,
	longitude float4 NULL,
	legal_entity_external_id varchar(255) NULL,
	site_external_id varchar(255) NULL,
	character_set varchar(255) NOT NULL,
	"language" varchar(255) NOT NULL,
	CONSTRAINT addresses_pkey PRIMARY KEY (id),
	CONSTRAINT uk_84200qt69jmxdl03jgxbjx2y0 UNIQUE (bpn),
	CONSTRAINT uk_jrhri6bt0pf6tjfingm6q69sq UNIQUE (uuid),
	CONSTRAINT uk_pqltsn49n4x8uk85jgysvsb8q UNIQUE (site_external_id),
	CONSTRAINT uk_qpesrk6tocdob1rtxhqnm95pt UNIQUE (legal_entity_external_id),
	CONSTRAINT uk_sq4iq64fq7nh87bedjanlpflr UNIQUE (external_id)
);


-- "bpdm-gate".identifier_status definition

-- Drop table

-- DROP TABLE "bpdm-gate".identifier_status;

CREATE TABLE "bpdm-gate".identifier_status (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"name" varchar(255) NOT NULL,
	technical_key varchar(255) NOT NULL,
	CONSTRAINT identifier_status_pkey PRIMARY KEY (id),
	CONSTRAINT uk_9cl4snfjrfw83vu2ngy17w47h UNIQUE (uuid),
	CONSTRAINT uk_l3v8y5eerkenti6d06t976c8f UNIQUE (technical_key)
);


-- "bpdm-gate".identifier_types definition

-- Drop table

-- DROP TABLE "bpdm-gate".identifier_types;

CREATE TABLE "bpdm-gate".identifier_types (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"name" varchar(255) NOT NULL,
	technical_key varchar(255) NOT NULL,
	url varchar(255) NULL,
	CONSTRAINT identifier_types_pkey PRIMARY KEY (id),
	CONSTRAINT uk_fnqds7oura6e9ctqgcaqvlp8l UNIQUE (uuid)
);


-- "bpdm-gate".issuing_bodies definition

-- Drop table

-- DROP TABLE "bpdm-gate".issuing_bodies;

CREATE TABLE "bpdm-gate".issuing_bodies (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"name" varchar(255) NOT NULL,
	technical_key varchar(255) NOT NULL,
	url varchar(255) NULL,
	CONSTRAINT issuing_bodies_pkey PRIMARY KEY (id),
	CONSTRAINT uk_5s7v0ym427b9i3ifcl81r0kx8 UNIQUE (uuid)
);


-- "bpdm-gate".legal_form_categories definition

-- Drop table

-- DROP TABLE "bpdm-gate".legal_form_categories;

CREATE TABLE "bpdm-gate".legal_form_categories (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"name" varchar(255) NOT NULL,
	url varchar(255) NULL,
	CONSTRAINT legal_form_categories_pkey PRIMARY KEY (id),
	CONSTRAINT uk_ibo9k8fk9qyxf2nxy9jl4n0dk UNIQUE (uuid)
);


-- "bpdm-gate".legal_forms definition

-- Drop table

-- DROP TABLE "bpdm-gate".legal_forms;

CREATE TABLE "bpdm-gate".legal_forms (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"language" int2 NOT NULL,
	abbreviation varchar(255) NULL,
	"name" varchar(255) NULL,
	technical_key varchar(255) NOT NULL,
	url varchar(255) NULL,
	CONSTRAINT legal_forms_pkey PRIMARY KEY (id),
	CONSTRAINT uk_towst0xttpmaixg7xk1ji5v2e UNIQUE (uuid)
);


-- "bpdm-gate".roles definition

-- Drop table

-- DROP TABLE "bpdm-gate".roles;

CREATE TABLE "bpdm-gate".roles (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"name" varchar(255) NOT NULL,
	technical_key varchar(255) NOT NULL,
	CONSTRAINT roles_pkey PRIMARY KEY (id),
	CONSTRAINT uk_bdys1vaxs0jqndxmixeragus8 UNIQUE (uuid),
	CONSTRAINT uk_homhokf01vc3v3vmr60acknce UNIQUE (technical_key)
);


-- "bpdm-gate".address_contexts definition

-- Drop table

-- DROP TABLE "bpdm-gate".address_contexts;

CREATE TABLE "bpdm-gate".address_contexts (
	address_id int8 NOT NULL,
	context varchar(255) NOT NULL,
	CONSTRAINT address_contexts_pkey PRIMARY KEY (address_id, context),
	CONSTRAINT fk1t127o73bpa2eh9vfmrtgrqum FOREIGN KEY (address_id) REFERENCES "bpdm-gate".addresses(id)
);
CREATE INDEX idxdyvn52j9bpwuaj3vqa31p1g75 ON "bpdm-gate".address_contexts USING btree (address_id);


-- "bpdm-gate".address_types definition

-- Drop table

-- DROP TABLE "bpdm-gate".address_types;

CREATE TABLE "bpdm-gate".address_types (
	address_id int8 NOT NULL,
	"type" varchar(255) NOT NULL,
	CONSTRAINT address_types_pkey PRIMARY KEY (address_id, type),
	CONSTRAINT fkjcajog5atsejjtaa487dmpopt FOREIGN KEY (address_id) REFERENCES "bpdm-gate".addresses(id)
);
CREATE INDEX idx1crcquumt5redip9eiycd75re ON "bpdm-gate".address_types USING btree (address_id);


-- "bpdm-gate".administrative_areas definition

-- Drop table

-- DROP TABLE "bpdm-gate".administrative_areas;

CREATE TABLE "bpdm-gate".administrative_areas (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	country varchar(255) NOT NULL,
	fips_code varchar(255) NULL,
	"language" varchar(255) NOT NULL,
	short_name varchar(255) NULL,
	"type" varchar(255) NOT NULL,
	value varchar(255) NOT NULL,
	address_id int8 NOT NULL,
	CONSTRAINT administrative_areas_pkey PRIMARY KEY (id),
	CONSTRAINT uk_oxh1af7od4k9ncu0qxwvdsol5 UNIQUE (uuid),
	CONSTRAINT fk49paeu46kjeqyfuiwrfchv8v6 FOREIGN KEY (address_id) REFERENCES "bpdm-gate".addresses(id)
);
CREATE INDEX idxl6bj1ysuij6hm73m0r8cypyd9 ON "bpdm-gate".administrative_areas USING btree (address_id);


-- "bpdm-gate".legal_entities definition

-- Drop table

-- DROP TABLE "bpdm-gate".legal_entities;

CREATE TABLE "bpdm-gate".legal_entities (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	bpn varchar(255) NOT NULL,
	currentness timestamp with time zone NOT NULL,
	external_id varchar(255) NOT NULL,
	legal_address_id int8 NOT NULL,
	legal_form_id int8 NULL,
	CONSTRAINT legal_entities_pkey PRIMARY KEY (id),
	CONSTRAINT uk_fn3cbtgn4mcc8qprvf6nc33rb UNIQUE (external_id),
	CONSTRAINT uk_kr9fdq30p6pldlbqktqd6g58c UNIQUE (uuid),
	CONSTRAINT uk_rs7me2py7dlci6yupo8bes1rc UNIQUE (bpn),
	CONSTRAINT fkbv0qw6w82eoj9sdrcrhelcevd FOREIGN KEY (legal_address_id) REFERENCES "bpdm-gate".addresses(id),
	CONSTRAINT fkc1b0nvelmc2p1j0g3co325pmv FOREIGN KEY (legal_form_id) REFERENCES "bpdm-gate".legal_forms(id)
);
CREATE INDEX idxm9ojfna20safop6xndvj1510n ON "bpdm-gate".legal_entities USING btree (legal_form_id);


-- "bpdm-gate".legal_entity_roles definition

-- Drop table

-- DROP TABLE "bpdm-gate".legal_entity_roles;

CREATE TABLE "bpdm-gate".legal_entity_roles (
	legal_entity_id int8 NOT NULL,
	role_id int8 NOT NULL,
	CONSTRAINT legal_entity_roles_pkey PRIMARY KEY (legal_entity_id, role_id),
	CONSTRAINT fkcr1umj7mb9hb3466vdfv8bk FOREIGN KEY (role_id) REFERENCES "bpdm-gate".roles(id),
	CONSTRAINT fkq8ddj33yps085xt53wgmn6g33 FOREIGN KEY (legal_entity_id) REFERENCES "bpdm-gate".legal_entities(id)
);
CREATE INDEX idxd5n7954r4y9rj77ru0vvxv35x ON "bpdm-gate".legal_entity_roles USING btree (legal_entity_id);


-- "bpdm-gate".legal_entity_types definition

-- Drop table

-- DROP TABLE "bpdm-gate".legal_entity_types;

CREATE TABLE "bpdm-gate".legal_entity_types (
	legal_entity_id int8 NOT NULL,
	"type" varchar(255) NOT NULL,
	CONSTRAINT legal_entity_types_pkey PRIMARY KEY (legal_entity_id, type),
	CONSTRAINT fkxfcusu50mh2877cu2g6qpjv9 FOREIGN KEY (legal_entity_id) REFERENCES "bpdm-gate".legal_entities(id)
);
CREATE INDEX idxl90kj28xlq30e0gdq06rwsl06 ON "bpdm-gate".legal_entity_types USING btree (legal_entity_id);


-- "bpdm-gate".legal_forms_legal_categories definition

-- Drop table

-- DROP TABLE "bpdm-gate".legal_forms_legal_categories;

CREATE TABLE "bpdm-gate".legal_forms_legal_categories (
	form_id int8 NOT NULL,
	category_id int8 NOT NULL,
	CONSTRAINT legal_forms_legal_categories_pkey PRIMARY KEY (form_id, category_id),
	CONSTRAINT fk2ewel4bp6u90u8ofokig30uyn FOREIGN KEY (form_id) REFERENCES "bpdm-gate".legal_forms(id),
	CONSTRAINT fk8tl7mijhb2hd6wv3qoaer2pe4 FOREIGN KEY (category_id) REFERENCES "bpdm-gate".legal_form_categories(id)
);
CREATE INDEX idx2vkekrtd36sp15dukkpjskh2m ON "bpdm-gate".legal_forms_legal_categories USING btree (form_id);
CREATE INDEX idx4976357rtrnbtoc7dlqhw66c9 ON "bpdm-gate".legal_forms_legal_categories USING btree (category_id);


-- "bpdm-gate".localities definition

-- Drop table

-- DROP TABLE "bpdm-gate".localities;

CREATE TABLE "bpdm-gate".localities (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"type" varchar(255) NOT NULL,
	short_name varchar(255) NULL,
	value varchar(255) NOT NULL,
	address_id int8 NOT NULL,
	CONSTRAINT localities_pkey PRIMARY KEY (id),
	CONSTRAINT uk_3glh6epgg5o1rnnsn39flp86g UNIQUE (uuid),
	CONSTRAINT fkmq18c84fij6erxjoqr72dn55w FOREIGN KEY (address_id) REFERENCES "bpdm-gate".addresses(id)
);
CREATE INDEX idxqsn69x58x1ks64jhmcs1l796k ON "bpdm-gate".localities USING btree (address_id);


-- "bpdm-gate".names definition

-- Drop table

-- DROP TABLE "bpdm-gate".names;

CREATE TABLE "bpdm-gate".names (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"language" varchar(255) NOT NULL,
	short_name varchar(255) NULL,
	"type" varchar(255) NOT NULL,
	value varchar(255) NOT NULL,
	legal_entity_id int8 NOT NULL,
	CONSTRAINT names_pkey PRIMARY KEY (id),
	CONSTRAINT uk_7mvq5d3rp4im8331kqiwkoyh7 UNIQUE (uuid),
	CONSTRAINT fk8hvn25nu8o2kc1mtesmac0rht FOREIGN KEY (legal_entity_id) REFERENCES "bpdm-gate".legal_entities(id)
);
CREATE INDEX idx2df02bfap6jciq17c370039qt ON "bpdm-gate".names USING btree (legal_entity_id);


-- "bpdm-gate".post_codes definition

-- Drop table

-- DROP TABLE "bpdm-gate".post_codes;

CREATE TABLE "bpdm-gate".post_codes (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"type" varchar(255) NOT NULL,
	value varchar(255) NOT NULL,
	address_id int8 NOT NULL,
	CONSTRAINT post_codes_pkey PRIMARY KEY (id),
	CONSTRAINT uk_rv95atjo0u6k0dps5ttfyhkyk UNIQUE (uuid),
	CONSTRAINT fkkvh4hu9mdq73q4jgb65qnwlyg FOREIGN KEY (address_id) REFERENCES "bpdm-gate".addresses(id)
);
CREATE INDEX idxjf0olkcrrt1dd3d5lc5mc34ft ON "bpdm-gate".post_codes USING btree (address_id);


-- "bpdm-gate".postal_delivery_points definition

-- Drop table

-- DROP TABLE "bpdm-gate".postal_delivery_points;

CREATE TABLE "bpdm-gate".postal_delivery_points (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"number" varchar(255) NULL,
	short_name varchar(255) NULL,
	"type" varchar(255) NOT NULL,
	value varchar(255) NOT NULL,
	address_id int8 NOT NULL,
	CONSTRAINT postal_delivery_points_pkey PRIMARY KEY (id),
	CONSTRAINT uk_owh54c21mdyq9s25s5gwrg4rd UNIQUE (uuid),
	CONSTRAINT fkkf08c4ahlaowtm108fm9f1js9 FOREIGN KEY (address_id) REFERENCES "bpdm-gate".addresses(id)
);
CREATE INDEX idxho27o3xxdc5b3yeur262pprq1 ON "bpdm-gate".postal_delivery_points USING btree (address_id);


-- "bpdm-gate".premises definition

-- Drop table

-- DROP TABLE "bpdm-gate".premises;

CREATE TABLE "bpdm-gate".premises (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	"number" varchar(255) NULL,
	short_name varchar(255) NULL,
	"type" varchar(255) NOT NULL,
	value varchar(255) NOT NULL,
	address_id int8 NOT NULL,
	CONSTRAINT premises_pkey PRIMARY KEY (id),
	CONSTRAINT uk_qyqbf4lw4lanekusfr3frkpio UNIQUE (uuid),
	CONSTRAINT fk6h5jk7ixcko5svfns3xj96hrr FOREIGN KEY (address_id) REFERENCES "bpdm-gate".addresses(id)
);
CREATE INDEX idxf1ppnm80ih8b4gqo91scd3k5k ON "bpdm-gate".premises USING btree (address_id);


-- "bpdm-gate".relations definition

-- Drop table

-- DROP TABLE "bpdm-gate".relations;

CREATE TABLE "bpdm-gate".relations (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	ended_at timestamp(6) NULL,
	"class" varchar(255) NOT NULL,
	started_at timestamp(6) NULL,
	"type" varchar(255) NOT NULL,
	end_node_id int8 NOT NULL,
	start_node_id int8 NOT NULL,
	CONSTRAINT relations_pkey PRIMARY KEY (id),
	CONSTRAINT uk_qylrhr8gciihnu82gvhwywwny UNIQUE (uuid),
	CONSTRAINT fk2ouq8tg0bvejr2o5oro5t4fav FOREIGN KEY (end_node_id) REFERENCES "bpdm-gate".legal_entities(id),
	CONSTRAINT fkej2415xh8whdtt0lvow1qbdur FOREIGN KEY (start_node_id) REFERENCES "bpdm-gate".legal_entities(id)
);
CREATE INDEX idx9kgukliy4u1s9woyd0tk1nmm3 ON "bpdm-gate".relations USING btree (start_node_id);
CREATE INDEX idxqhf4eyra7qpla4kh89miwsnv1 ON "bpdm-gate".relations USING btree (end_node_id);


-- "bpdm-gate".sites definition

-- Drop table

-- DROP TABLE "bpdm-gate".sites;

CREATE TABLE "bpdm-gate".sites (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	bpn varchar(255) NOT NULL,
	external_id varchar(255) NOT NULL,
	legal_entity_external_id varchar(255) NULL,
	"name" varchar(255) NOT NULL,
	legal_entity_id int8 NOT NULL,
	main_address_id int8 NOT NULL,
	CONSTRAINT sites_pkey PRIMARY KEY (id),
	CONSTRAINT uk_1vrdeiex4x7p93r5svtvb5b4x UNIQUE (external_id),
	CONSTRAINT uk_27n4pihn8c6rh8v202wevppyu UNIQUE (bpn),
	CONSTRAINT uk_b2t72lxjqja93ids61sgvo4hg UNIQUE (uuid),
	CONSTRAINT uk_i6tphp1o267d101t28iqqii3s UNIQUE (legal_entity_external_id),
	CONSTRAINT fk7xchgecrp1q3mp9rvdwow10e9 FOREIGN KEY (main_address_id) REFERENCES "bpdm-gate".addresses(id),
	CONSTRAINT fkqrcx27qia8f1w8c7d3afdraxq FOREIGN KEY (legal_entity_id) REFERENCES "bpdm-gate".legal_entities(id)
);


-- "bpdm-gate".thoroughfares definition

-- Drop table

-- DROP TABLE "bpdm-gate".thoroughfares;

CREATE TABLE "bpdm-gate".thoroughfares (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	direction varchar(255) NULL,
	"name" varchar(255) NULL,
	"number" varchar(255) NULL,
	short_name varchar(255) NULL,
	"type" varchar(255) NOT NULL,
	value varchar(255) NOT NULL,
	address_id int8 NOT NULL,
	CONSTRAINT thoroughfares_pkey PRIMARY KEY (id),
	CONSTRAINT uk_krwm2oeut81dh9vtok83wpyj1 UNIQUE (uuid),
	CONSTRAINT fkdsy1ybjfggn6vqgoqblwsrj58 FOREIGN KEY (address_id) REFERENCES "bpdm-gate".addresses(id)
);
CREATE INDEX idxr37crovik7xrvr8mfk9wfimmh ON "bpdm-gate".thoroughfares USING btree (address_id);


-- "bpdm-gate".address_partners definition

-- Drop table

-- DROP TABLE "bpdm-gate".address_partners;

CREATE TABLE "bpdm-gate".address_partners (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	bpn varchar(255) NULL,
	address_id int8 NULL,
	legal_entity_id int8 NULL,
	site_id int8 NULL,
	CONSTRAINT address_partners_pkey PRIMARY KEY (id),
	CONSTRAINT uk_548sfm83c0bx2o4xlfdv8vd41 UNIQUE (uuid),
	CONSTRAINT uk_a1ie8abutkcme24w8jg8n8gse UNIQUE (bpn),
	CONSTRAINT fk2q0l85gdqavp1rj0tvy9ibi6g FOREIGN KEY (legal_entity_id) REFERENCES "bpdm-gate".legal_entities(id),
	CONSTRAINT fkeckp75l30jqeaqhu3jc5c6sd0 FOREIGN KEY (site_id) REFERENCES "bpdm-gate".sites(id),
	CONSTRAINT fkncbx5e4i55su8yeh48vnjgnwt FOREIGN KEY (address_id) REFERENCES "bpdm-gate".addresses(id)
);
CREATE INDEX idxekeyc5bebiebgxgyfod241rkg ON "bpdm-gate".address_partners USING btree (site_id);
CREATE INDEX idxitgfpbcg97xng4hpyvx72oj0y ON "bpdm-gate".address_partners USING btree (legal_entity_id);


-- "bpdm-gate".bank_accounts definition

-- Drop table

-- DROP TABLE "bpdm-gate".bank_accounts;

CREATE TABLE "bpdm-gate".bank_accounts (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	currency varchar(255) NOT NULL,
	international_account_identifier varchar(255) NULL,
	international_bank_identifier varchar(255) NULL,
	national_account_identifier varchar(255) NULL,
	national_bank_identifier varchar(255) NULL,
	legal_entity_id int8 NOT NULL,
	CONSTRAINT bank_accounts_pkey PRIMARY KEY (id),
	CONSTRAINT uk_13csuryu9jiclku4v0cp8j0ke UNIQUE (uuid),
	CONSTRAINT fk603c8w0jfdxnfhp6jgka1e1yx FOREIGN KEY (legal_entity_id) REFERENCES "bpdm-gate".legal_entities(id)
);
CREATE INDEX idx8yh3qu1wpem5urty6p6cepsaq ON "bpdm-gate".bank_accounts USING btree (legal_entity_id);


-- "bpdm-gate".business_stati definition

-- Drop table

-- DROP TABLE "bpdm-gate".business_stati;

CREATE TABLE "bpdm-gate".business_stati (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	denotation varchar(255) NULL,
	"type" int2 NOT NULL,
	valid_from timestamp(6) NULL,
	valid_to timestamp(6) NULL,
	legal_entity_id int8 NOT NULL,
	CONSTRAINT business_stati_pkey PRIMARY KEY (id),
	CONSTRAINT uk_7q20mh3x4l1dnmvo8y3uhfmxp UNIQUE (uuid),
	CONSTRAINT fkdy68ipb902ena4s7f4c2tb818 FOREIGN KEY (legal_entity_id) REFERENCES "bpdm-gate".legal_entities(id)
);
CREATE INDEX idxre5j6xugjkooe2sw4xoxpgh2u ON "bpdm-gate".business_stati USING btree (legal_entity_id);


-- "bpdm-gate".classifications definition

-- Drop table

-- DROP TABLE "bpdm-gate".classifications;

CREATE TABLE "bpdm-gate".classifications (
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
	CONSTRAINT fk5b0t7pv9g32ifp4kew3ubxbft FOREIGN KEY (legal_entity_id) REFERENCES "bpdm-gate".legal_entities(id)
);
CREATE INDEX idx6g01nxco0d10y4an0kt91rwp6 ON "bpdm-gate".classifications USING btree (legal_entity_id);


-- "bpdm-gate".identifiers definition

-- Drop table

-- DROP TABLE "bpdm-gate".identifiers;

CREATE TABLE "bpdm-gate".identifiers (
	id int8 NOT NULL,
	created_at timestamp with time zone NOT NULL,
	updated_at timestamp with time zone NOT NULL,
	uuid uuid NOT NULL,
	value varchar(255) NOT NULL,
	issuing_body_id int8 NULL,
	legal_entity_id int8 NOT NULL,
	status int8 NULL,
	type_id int8 NOT NULL,
	CONSTRAINT identifiers_pkey PRIMARY KEY (id),
	CONSTRAINT uk_4gq2scuptygn5w22oa4njv1sr UNIQUE (uuid),
	CONSTRAINT fkjfveo4srfy8bp1jgwhncb9uf FOREIGN KEY (type_id) REFERENCES "bpdm-gate".identifier_types(id),
	CONSTRAINT fkjvit2agtrlje15stw0rriehmu FOREIGN KEY (status) REFERENCES "bpdm-gate".identifier_status(id),
	CONSTRAINT fkn1f88gsu2nn7l9mqpfa4bfxc7 FOREIGN KEY (issuing_body_id) REFERENCES "bpdm-gate".issuing_bodies(id),
	CONSTRAINT fknjjfe5ispcevxlvciqgau80w FOREIGN KEY (legal_entity_id) REFERENCES "bpdm-gate".legal_entities(id)
);
CREATE INDEX idx165b3dmxtvf0gv4ddvr1qke3b ON "bpdm-gate".identifiers USING btree (status);
CREATE INDEX idxcor8gn1asqjwtiifvckomktgl ON "bpdm-gate".identifiers USING btree (issuing_body_id);
CREATE INDEX idxrqhrpfkxnhiplfx4omxyvvbyv ON "bpdm-gate".identifiers USING btree (type_id);
CREATE INDEX idxy52013s3vtpghrehn526vi82 ON "bpdm-gate".identifiers USING btree (legal_entity_id);


-- "bpdm-gate".bank_account_trust_scores definition

-- Drop table

-- DROP TABLE "bpdm-gate".bank_account_trust_scores;

CREATE TABLE "bpdm-gate".bank_account_trust_scores (
	account_id int8 NOT NULL,
	score float4 NOT NULL,
	CONSTRAINT bank_account_trust_scores_pkey PRIMARY KEY (account_id, score),
	CONSTRAINT fkimac9uoxfa5tltlf5u356rps9 FOREIGN KEY (account_id) REFERENCES "bpdm-gate".bank_accounts(id)
);
CREATE INDEX idxefw11vgnqp4gjeogmmuja0poc ON "bpdm-gate".bank_account_trust_scores USING btree (account_id);
