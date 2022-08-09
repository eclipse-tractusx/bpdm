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

CREATE TABLE sync_records
(
    id             BIGINT                      NOT NULL,
    uuid           UUID                        NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    type           VARCHAR(255)                NOT NULL,
    status         VARCHAR(255)                NOT NULL,
    progress       FLOAT                       NOT NULL,
    count          INTEGER                     NOT NULL,
    status_details VARCHAR(255),
    save_state     VARCHAR(255),
    started_at     TIMESTAMP with time zone,
    finished_at    TIMESTAMP with time zone,
    CONSTRAINT pk_sync_records PRIMARY KEY (id)
);

ALTER TABLE sync_records
    ADD CONSTRAINT uc_sync_records_type UNIQUE (type);

ALTER TABLE sync_records
    ADD CONSTRAINT uc_sync_records_uuid UNIQUE (uuid);