/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc

import com.fasterxml.jackson.annotation.JsonIgnore

const val LEGAL_ENTITIES_INDEX_NAME = "legal-entities"
const val MAPPINGS_FILE_PATH_LEGAL_ENTITIES = "opensearch/index-mappings-legal-entities.json"

data class LegalEntityDoc(
    @JsonIgnore // ignore since this is the id and does not need to be in the document source
    val bpn: String,
    val names: Collection<TextDoc>,
    val legalForm: TextDoc?,
    val status: TextDoc?,
    val addresses: Collection<AddressDoc>,
    val classifications: Collection<TextDoc>,
    val sites: Collection<TextDoc>
)
