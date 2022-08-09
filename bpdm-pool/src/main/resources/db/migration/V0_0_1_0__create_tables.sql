/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

CREATE SEQUENCE IF NOT EXISTS bpdm_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE address_contexts
(
    address_id BIGINT       NOT NULL,
    context    VARCHAR(255) NOT NULL
);

CREATE TABLE address_types
(
    address_id BIGINT       NOT NULL,
    type       VARCHAR(255) NOT NULL
);

CREATE TABLE address_versions
(
    id            BIGINT                      NOT NULL,
    uuid          UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    character_set INTEGER                     NOT NULL,
    language      INTEGER                     NOT NULL,
    CONSTRAINT pk_address_versions PRIMARY KEY (id)
);

CREATE TABLE addresses
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    care_of    VARCHAR(255),
    country    VARCHAR(255)                NOT NULL,
    version_id BIGINT                      NOT NULL,
    partner_id BIGINT                      NOT NULL,
    latitude   FLOAT,
    longitude  FLOAT,
    altitude   FLOAT,
    CONSTRAINT pk_addresses PRIMARY KEY (id)
);

CREATE TABLE administrative_areas
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    value      VARCHAR(255)                NOT NULL,
    short_name VARCHAR(255),
    fips_code  VARCHAR(255),
    type       VARCHAR(255)                NOT NULL,
    language   VARCHAR(255)                NOT NULL,
    country    VARCHAR(255)                NOT NULL,
    address_id BIGINT                      NOT NULL,
    CONSTRAINT pk_administrative_areas PRIMARY KEY (id)
);

CREATE TABLE bank_account_trust_scores
(
    account_id BIGINT NOT NULL,
    score      FLOAT  NOT NULL
);

CREATE TABLE bank_accounts
(
    id                               BIGINT                      NOT NULL,
    uuid                             UUID                        NOT NULL,
    created_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    currency                         VARCHAR(255)                NOT NULL,
    international_account_identifier VARCHAR(255)                NOT NULL,
    international_bank_identifier    VARCHAR(255)                NOT NULL,
    national_account_identifier      VARCHAR(255)                NOT NULL,
    national_bank_identifier         VARCHAR(255)                NOT NULL,
    partner_id                       BIGINT                      NOT NULL,
    CONSTRAINT pk_bank_accounts PRIMARY KEY (id)
);

CREATE TABLE business_partner_types
(
    partner_id BIGINT       NOT NULL,
    type       VARCHAR(255) NOT NULL
);

CREATE TABLE business_partners
(
    id            BIGINT                      NOT NULL,
    uuid          UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    bpn           VARCHAR(255)                NOT NULL,
    legal_form_id BIGINT,
    CONSTRAINT pk_business_partners PRIMARY KEY (id)
);

CREATE TABLE business_partners_roles
(
    partner_id BIGINT NOT NULL,
    role_id    BIGINT NOT NULL,
    CONSTRAINT pk_business_partners_roles PRIMARY KEY (partner_id, role_id)
);

CREATE TABLE business_stati
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    denotation VARCHAR(255)                NOT NULL,
    valid_from TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    valid_to   TIMESTAMP WITHOUT TIME ZONE,
    type       INTEGER                     NOT NULL,
    partner_id BIGINT                      NOT NULL,
    CONSTRAINT pk_business_stati PRIMARY KEY (id)
);

CREATE TABLE care_ofs
(
    id         BIGINT                      NOT NULL,
    value      VARCHAR(255)                NOT NULL,
    short_name VARCHAR(255),
    number     INTEGER,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_care_ofs PRIMARY KEY (id)
);

CREATE TABLE classifications
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    value      VARCHAR(255)                NOT NULL,
    code       VARCHAR(255),
    type       VARCHAR(255),
    partner_id BIGINT                      NOT NULL,
    CONSTRAINT pk_classifications PRIMARY KEY (id)
);

CREATE TABLE configuration_entries
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    key        VARCHAR(255)                NOT NULL,
    value      VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_configuration_entries PRIMARY KEY (id)
);

CREATE TABLE identifier_status
(
    id            BIGINT                      NOT NULL,
    uuid          UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    name          VARCHAR(255)                NOT NULL,
    technical_key VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_identifier_status PRIMARY KEY (id)
);

CREATE TABLE identifier_types
(
    id            BIGINT                      NOT NULL,
    uuid          UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    name          VARCHAR(255)                NOT NULL,
    url           VARCHAR(255),
    technical_key VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_identifier_types PRIMARY KEY (id)
);

CREATE TABLE identifiers
(
    id              BIGINT                      NOT NULL,
    uuid            UUID                        NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    value           VARCHAR(255)                NOT NULL,
    type_id         BIGINT                      NOT NULL,
    status          BIGINT,
    issuing_body_id BIGINT,
    partner_id      BIGINT                      NOT NULL,
    CONSTRAINT pk_identifiers PRIMARY KEY (id)
);

CREATE TABLE issuing_bodies
(
    id            BIGINT                      NOT NULL,
    uuid          UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    name          VARCHAR(255)                NOT NULL,
    url           VARCHAR(255),
    technical_key VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_issuing_bodies PRIMARY KEY (id)
);

CREATE TABLE legal_form_categories
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    name       VARCHAR(255)                NOT NULL,
    url        VARCHAR(255),
    CONSTRAINT pk_legal_form_categories PRIMARY KEY (id)
);

CREATE TABLE legal_forms
(
    id            BIGINT                      NOT NULL,
    uuid          UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    name          VARCHAR(255)                NOT NULL,
    url           VARCHAR(255),
    language      INTEGER                     NOT NULL,
    abbreviation  VARCHAR(255),
    technical_key VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_legal_forms PRIMARY KEY (id)
);

CREATE TABLE legal_forms_legal_categories
(
    category_id BIGINT NOT NULL,
    form_id     BIGINT NOT NULL,
    CONSTRAINT pk_legal_forms_legal_categories PRIMARY KEY (category_id, form_id)
);

CREATE TABLE localities
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    value      VARCHAR(255)                NOT NULL,
    short_name VARCHAR(255),
    type       VARCHAR(255)                NOT NULL,
    language   VARCHAR(255)                NOT NULL,
    address_id BIGINT                      NOT NULL,
    CONSTRAINT pk_localities PRIMARY KEY (id)
);

CREATE TABLE names
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    value      VARCHAR(255)                NOT NULL,
    short_name VARCHAR(255),
    type       VARCHAR(255)                NOT NULL,
    language   VARCHAR(255)                NOT NULL,
    partner_id BIGINT                      NOT NULL,
    CONSTRAINT pk_names PRIMARY KEY (id)
);

CREATE TABLE post_codes
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    value      VARCHAR(255)                NOT NULL,
    type       VARCHAR(255)                NOT NULL,
    country    VARCHAR(255)                NOT NULL,
    address_id BIGINT                      NOT NULL,
    CONSTRAINT pk_post_codes PRIMARY KEY (id)
);

CREATE TABLE postal_delivery_points
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    value      VARCHAR(255)                NOT NULL,
    short_name VARCHAR(255),
    number     VARCHAR(255),
    type       VARCHAR(255)                NOT NULL,
    language   VARCHAR(255)                NOT NULL,
    address_id BIGINT                      NOT NULL,
    CONSTRAINT pk_postal_delivery_points PRIMARY KEY (id)
);

CREATE TABLE premises
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    value      VARCHAR(255)                NOT NULL,
    short_name VARCHAR(255),
    number     VARCHAR(255),
    type       VARCHAR(255)                NOT NULL,
    language   VARCHAR(255)                NOT NULL,
    address_id BIGINT                      NOT NULL,
    CONSTRAINT pk_premises PRIMARY KEY (id)
);

CREATE TABLE relations
(
    id            BIGINT                      NOT NULL,
    uuid          UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    class         VARCHAR(255)                NOT NULL,
    type          VARCHAR(255)                NOT NULL,
    start_node_id BIGINT                      NOT NULL,
    end_node_id   BIGINT                      NOT NULL,
    started_at    TIMESTAMP WITHOUT TIME ZONE,
    ended_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_relations PRIMARY KEY (id)
);

CREATE TABLE roles
(
    id            BIGINT                      NOT NULL,
    uuid          UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    technical_key VARCHAR(255)                NOT NULL,
    name          VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE thoroughfares
(
    id         BIGINT                      NOT NULL,
    uuid       UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    value      VARCHAR(255)                NOT NULL,
    name       VARCHAR(255),
    short_name VARCHAR(255),
    number     VARCHAR(255),
    direction  VARCHAR(255),
    type       VARCHAR(255)                NOT NULL,
    language   VARCHAR(255)                NOT NULL,
    address_id BIGINT                      NOT NULL,
    CONSTRAINT pk_thoroughfares PRIMARY KEY (id)
);

ALTER TABLE address_versions
    ADD CONSTRAINT uc_address_versions_uuid UNIQUE (uuid);

ALTER TABLE addresses
    ADD CONSTRAINT uc_addresses_uuid UNIQUE (uuid);

ALTER TABLE administrative_areas
    ADD CONSTRAINT uc_administrative_areas_uuid UNIQUE (uuid);

ALTER TABLE bank_accounts
    ADD CONSTRAINT uc_bank_accounts_uuid UNIQUE (uuid);

ALTER TABLE business_partners
    ADD CONSTRAINT uc_business_partners_bpn UNIQUE (bpn);

ALTER TABLE business_partners
    ADD CONSTRAINT uc_business_partners_uuid UNIQUE (uuid);

ALTER TABLE business_stati
    ADD CONSTRAINT uc_business_stati_uuid UNIQUE (uuid);

ALTER TABLE care_ofs
    ADD CONSTRAINT uc_care_ofs_uuid UNIQUE (uuid);

ALTER TABLE classifications
    ADD CONSTRAINT uc_classifications_uuid UNIQUE (uuid);

ALTER TABLE configuration_entries
    ADD CONSTRAINT uc_configuration_entries_key UNIQUE (key);

ALTER TABLE configuration_entries
    ADD CONSTRAINT uc_configuration_entries_uuid UNIQUE (uuid);

ALTER TABLE identifier_status
    ADD CONSTRAINT uc_identifier_status_technicalkey UNIQUE (technical_key);

ALTER TABLE identifier_status
    ADD CONSTRAINT uc_identifier_status_uuid UNIQUE (uuid);

ALTER TABLE identifier_types
    ADD CONSTRAINT uc_identifier_types_uuid UNIQUE (uuid);

ALTER TABLE identifiers
    ADD CONSTRAINT uc_identifiers_uuid UNIQUE (uuid);

ALTER TABLE issuing_bodies
    ADD CONSTRAINT uc_issuing_bodies_uuid UNIQUE (uuid);

ALTER TABLE legal_form_categories
    ADD CONSTRAINT uc_legal_form_categories_uuid UNIQUE (uuid);

ALTER TABLE legal_forms
    ADD CONSTRAINT uc_legal_forms_uuid UNIQUE (uuid);

ALTER TABLE localities
    ADD CONSTRAINT uc_localities_uuid UNIQUE (uuid);

ALTER TABLE names
    ADD CONSTRAINT uc_names_uuid UNIQUE (uuid);

ALTER TABLE post_codes
    ADD CONSTRAINT uc_post_codes_uuid UNIQUE (uuid);

ALTER TABLE postal_delivery_points
    ADD CONSTRAINT uc_postal_delivery_points_uuid UNIQUE (uuid);

ALTER TABLE premises
    ADD CONSTRAINT uc_premises_uuid UNIQUE (uuid);

ALTER TABLE relations
    ADD CONSTRAINT uc_relations_uuid UNIQUE (uuid);

ALTER TABLE roles
    ADD CONSTRAINT uc_roles_technical_key UNIQUE (technical_key);

ALTER TABLE roles
    ADD CONSTRAINT uc_roles_uuid UNIQUE (uuid);

ALTER TABLE thoroughfares
    ADD CONSTRAINT uc_thoroughfares_uuid UNIQUE (uuid);

ALTER TABLE addresses
    ADD CONSTRAINT FK_ADDRESSES_ON_PARTNER FOREIGN KEY (partner_id) REFERENCES business_partners (id);

ALTER TABLE addresses
    ADD CONSTRAINT FK_ADDRESSES_ON_VERSION FOREIGN KEY (version_id) REFERENCES address_versions (id);

ALTER TABLE administrative_areas
    ADD CONSTRAINT FK_ADMINISTRATIVE_AREAS_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

ALTER TABLE bank_accounts
    ADD CONSTRAINT FK_BANK_ACCOUNTS_ON_PARTNER FOREIGN KEY (partner_id) REFERENCES business_partners (id);

ALTER TABLE business_partners
    ADD CONSTRAINT FK_BUSINESS_PARTNERS_ON_LEGAL_FORM FOREIGN KEY (legal_form_id) REFERENCES legal_forms (id);

ALTER TABLE business_stati
    ADD CONSTRAINT FK_BUSINESS_STATI_ON_PARTNER FOREIGN KEY (partner_id) REFERENCES business_partners (id);

ALTER TABLE classifications
    ADD CONSTRAINT FK_CLASSIFICATIONS_ON_PARTNER FOREIGN KEY (partner_id) REFERENCES business_partners (id);

ALTER TABLE identifiers
    ADD CONSTRAINT FK_IDENTIFIERS_ON_ISSUING_BODY FOREIGN KEY (issuing_body_id) REFERENCES issuing_bodies (id);

ALTER TABLE identifiers
    ADD CONSTRAINT FK_IDENTIFIERS_ON_PARTNER FOREIGN KEY (partner_id) REFERENCES business_partners (id);

ALTER TABLE identifiers
    ADD CONSTRAINT FK_IDENTIFIERS_ON_STATUS FOREIGN KEY (status) REFERENCES identifier_status (id);

ALTER TABLE identifiers
    ADD CONSTRAINT FK_IDENTIFIERS_ON_TYPE FOREIGN KEY (type_id) REFERENCES identifier_types (id);

ALTER TABLE localities
    ADD CONSTRAINT FK_LOCALITIES_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

ALTER TABLE names
    ADD CONSTRAINT FK_NAMES_ON_PARTNER FOREIGN KEY (partner_id) REFERENCES business_partners (id);

ALTER TABLE postal_delivery_points
    ADD CONSTRAINT FK_POSTAL_DELIVERY_POINTS_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

ALTER TABLE post_codes
    ADD CONSTRAINT FK_POST_CODES_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

ALTER TABLE premises
    ADD CONSTRAINT FK_PREMISES_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

ALTER TABLE relations
    ADD CONSTRAINT FK_RELATIONS_ON_END_NODE FOREIGN KEY (end_node_id) REFERENCES business_partners (id);

ALTER TABLE relations
    ADD CONSTRAINT FK_RELATIONS_ON_START_NODE FOREIGN KEY (start_node_id) REFERENCES business_partners (id);

ALTER TABLE thoroughfares
    ADD CONSTRAINT FK_THOROUGHFARES_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

ALTER TABLE address_contexts
    ADD CONSTRAINT fk_address_contexts_on_address FOREIGN KEY (address_id) REFERENCES addresses (id);

ALTER TABLE address_types
    ADD CONSTRAINT fk_address_types_on_address FOREIGN KEY (address_id) REFERENCES addresses (id);

ALTER TABLE bank_account_trust_scores
    ADD CONSTRAINT fk_bank_account_trust_scores_on_bank_account FOREIGN KEY (account_id) REFERENCES bank_accounts (id);

ALTER TABLE business_partner_types
    ADD CONSTRAINT fk_business_partner_types_on_business_partner FOREIGN KEY (partner_id) REFERENCES business_partners (id);

ALTER TABLE business_partners_roles
    ADD CONSTRAINT fk_busparrol_on_business_partner FOREIGN KEY (partner_id) REFERENCES business_partners (id);

ALTER TABLE business_partners_roles
    ADD CONSTRAINT fk_busparrol_on_role FOREIGN KEY (role_id) REFERENCES roles (id);

ALTER TABLE legal_forms_legal_categories
    ADD CONSTRAINT fk_legforlegcat_on_legal_form FOREIGN KEY (form_id) REFERENCES legal_forms (id);

ALTER TABLE legal_forms_legal_categories
    ADD CONSTRAINT fk_legforlegcat_on_legal_form_category FOREIGN KEY (category_id) REFERENCES legal_form_categories (id);