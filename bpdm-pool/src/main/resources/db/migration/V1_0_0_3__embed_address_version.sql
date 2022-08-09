ALTER TABLE addresses
    DROP version_id CASCADE;

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

/* Character set and languages were wrongly stored as Integer before.
   Create new columns as VARCHAR but don't migrate old data as CDQ has backup. */
ALTER TABLE addresses
    ADD character_set VARCHAR(255) NOT NULL DEFAULT 'UNDEFINED';

ALTER TABLE addresses
    ADD language VARCHAR(255) NOT NULL DEFAULT 'undefined';


DROP TABLE address_versions CASCADE;

