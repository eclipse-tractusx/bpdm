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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc

import com.fasterxml.jackson.annotation.JsonIgnore

const val ADDRESS_PARTNER_INDEX_NAME = "address-partner"
const val MAPPINGS_FILE_PATH_ADDRESSES = "opensearch/index-mappings-addresses.json"

data class AddressPartnerDoc(
    @JsonIgnore // ignore since this is the id and does not need to be in the document source
    val bpn: String,
    val administrativeAreas: Collection<String>,
    val postCodes: Collection<String>,
    val localities: Collection<String>,
    val thoroughfares: Collection<String>,
    val premises: Collection<String>,
    val postalDeliveryPoints: Collection<String>,
    val countryCode: Collection<String>
)
