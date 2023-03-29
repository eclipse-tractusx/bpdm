-- Addresses
CREATE TABLE addresses
  (
     id                       INT8 NOT NULL,
     created_at               TIMESTAMP WITH time zone NOT NULL,
     updated_at               TIMESTAMP WITH time zone NOT NULL,
     uuid                     UUID NOT NULL,
     bpn                      VARCHAR(255) NULL,
     care_of                  VARCHAR(255) NULL,
     country                  VARCHAR(255) NOT NULL,
     external_id              VARCHAR(255) NOT NULL,
     altitude                 FLOAT4 NULL,
     latitude                 FLOAT4 NULL,
     longitude                FLOAT4 NULL,
     legal_entity_external_id VARCHAR(255) NULL,
     site_external_id         VARCHAR(255) NULL,
     character_set            VARCHAR(255) NOT NULL,
     "language"               VARCHAR(255) NOT NULL,
     CONSTRAINT pk_addresses PRIMARY KEY (id),
     CONSTRAINT uc_addresses_uuid UNIQUE (uuid),
     CONSTRAINT uc_addresses_external_id UNIQUE (external_id)
  );

--Address Type
CREATE TABLE address_types
  (
     address_id INT8 NOT NULL,
     "type"     VARCHAR(255) NOT NULL,
     CONSTRAINT pk_address_types PRIMARY KEY (address_id, type),
     CONSTRAINT fk_address_types_on_address FOREIGN KEY (address_id) REFERENCES
     addresses(id)
  );
CREATE INDEX idx_1crcquumt5redip9eiycd75re ON address_types USING btree (address_id);

-- Address Context
CREATE TABLE address_contexts
  (
     address_id INT8 NOT NULL,
     context    VARCHAR(255) NOT NULL,
     CONSTRAINT pk_address_contexts PRIMARY KEY (address_id, context),
     CONSTRAINT fk_address_contexts_on_address FOREIGN KEY (address_id)
     REFERENCES addresses(id)
  );
CREATE INDEX idx_dyvn52j9bpwuaj3vqa31p1g75 ON address_contexts USING btree (address_id);

-- Administrative Areas
CREATE TABLE administrative_areas
  (
     id         INT8 NOT NULL,
     created_at TIMESTAMP WITH time zone NOT NULL,
     updated_at TIMESTAMP WITH time zone NOT NULL,
     uuid       UUID NOT NULL,
     country    VARCHAR(255) NOT NULL,
     fips_code  VARCHAR(255) NULL,
     "language" VARCHAR(255) NOT NULL,
     short_name VARCHAR(255) NULL,
     "type"     VARCHAR(255) NOT NULL,
     value      VARCHAR(255) NOT NULL,
     address_id INT8 NOT NULL,
     CONSTRAINT pk_administrative_areas PRIMARY KEY (id),
     CONSTRAINT uc_administrative_areas_uuid UNIQUE (uuid),
     CONSTRAINT fk_administrative_areas_on_address FOREIGN KEY (address_id)
     REFERENCES addresses(id)
  );
CREATE INDEX idx_l6bj1ysuij6hm73m0r8cypyd9 ON administrative_areas USING btree (address_id);

-- Localities Table
CREATE TABLE localities (
	id INT8                   NOT NULL,
	created_at                TIMESTAMP WITH time zone NOT NULL,
	updated_at                TIMESTAMP WITH time zone NOT NULL,
	uuid UUID                 NOT NULL,
	"type"                    VARCHAR(255) NOT NULL,
	short_name                VARCHAR(255) NULL,
	value                     VARCHAR(255) NOT NULL,
	address_id INT8           NOT NULL,
	CONSTRAINT pk_localities  PRIMARY KEY (id)
);

CREATE INDEX idx_qsn69x58x1ks64jhmcs1l796k ON localities USING btree (address_id);

ALTER TABLE localities
    ADD CONSTRAINT uc_localities_uuid UNIQUE (uuid);

ALTER TABLE localities
    ADD CONSTRAINT FK_LOCALITIES_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

-- Postal Delivery Points Table
CREATE TABLE postal_delivery_points
  (
     id         INT8 NOT NULL,
     created_at TIMESTAMP WITH time zone NOT NULL,
     updated_at TIMESTAMP WITH time zone NOT NULL,
     uuid       UUID NOT NULL,
     "number"   VARCHAR(255) NULL,
     short_name VARCHAR(255) NULL,
     "type"     VARCHAR(255) NOT NULL,
     value      VARCHAR(255) NOT NULL,
     address_id INT8 NOT NULL,
     CONSTRAINT pk_postal_delivery_points PRIMARY KEY (id)
  );

CREATE INDEX idx_ho27o3xxdc5b3yeur262pprq1 ON postal_delivery_points USING btree (address_id);

ALTER TABLE postal_delivery_points
    ADD CONSTRAINT uc_postal_delivery_points_uuid UNIQUE (uuid);

ALTER TABLE postal_delivery_points
    ADD CONSTRAINT FK_POSTAL_DELIVERY_POINTS_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

-- Post Codes Table
CREATE TABLE post_codes
  (
     id         INT8 NOT NULL,
     created_at TIMESTAMP WITH time zone NOT NULL,
     updated_at TIMESTAMP WITH time zone NOT NULL,
     uuid       UUID NOT NULL,
     "type"     VARCHAR(255) NOT NULL,
     value      VARCHAR(255) NOT NULL,
     address_id INT8 NOT NULL,
     CONSTRAINT pk_post_codes PRIMARY KEY (id)
  );
CREATE INDEX idx_jf0olkcrrt1dd3d5lc5mc34ft ON post_codes USING btree (address_id);

ALTER TABLE post_codes
    ADD CONSTRAINT uc_post_codes_uuid UNIQUE (uuid);

ALTER TABLE post_codes
    ADD CONSTRAINT FK_POST_CODES_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

-- Premises Table
CREATE TABLE premises
  (
     id         INT8 NOT NULL,
     created_at TIMESTAMP WITH time zone NOT NULL,
     updated_at TIMESTAMP WITH time zone NOT NULL,
     uuid       UUID NOT NULL,
     "number"   VARCHAR(255) NULL,
     short_name VARCHAR(255) NULL,
     "type"     VARCHAR(255) NOT NULL,
     value      VARCHAR(255) NOT NULL,
     address_id INT8 NOT NULL,
     CONSTRAINT pk_premises PRIMARY KEY (id)
  );

CREATE INDEX idx_f1ppnm80ih8b4gqo91scd3k5k ON premises USING btree (address_id);

ALTER TABLE premises
    ADD CONSTRAINT uc_premises_uuid UNIQUE (uuid);

ALTER TABLE premises
    ADD CONSTRAINT FK_PREMISES_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

-- Thoroughfares Table
CREATE TABLE thoroughfares
  (
     id         INT8 NOT NULL,
     created_at TIMESTAMP WITH time zone NOT NULL,
     updated_at TIMESTAMP WITH time zone NOT NULL,
     uuid       UUID NOT NULL,
     direction  VARCHAR(255) NULL,
     "name"     VARCHAR(255) NULL,
     "number"   VARCHAR(255) NULL,
     short_name VARCHAR(255) NULL,
     "type"     VARCHAR(255) NOT NULL,
     value      VARCHAR(255) NOT NULL,
     address_id INT8 NOT NULL,
     CONSTRAINT pk_thoroughfares PRIMARY KEY (id)
  );
CREATE INDEX idx_r37crovik7xrvr8mfk9wfimmh ON thoroughfares USING btree (address_id);

ALTER TABLE thoroughfares
    ADD CONSTRAINT uc_thoroughfares_uuid UNIQUE (uuid);

ALTER TABLE thoroughfares
    ADD CONSTRAINT FK_THOROUGHFARES_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);










