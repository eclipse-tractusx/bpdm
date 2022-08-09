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

CREATE TABLE partner_changelog_entries
(
    id             BIGINT       NOT NULL,
    uuid           UUID         NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    changelog_type VARCHAR(255) NOT NULL,
    bpn            VARCHAR(255) NOT NULL,
    CONSTRAINT pk_partner_changelog_entries PRIMARY KEY (id)
);

ALTER TABLE partner_changelog_entries
    ADD CONSTRAINT uc_partner_changelog_entries_uuid UNIQUE (uuid);