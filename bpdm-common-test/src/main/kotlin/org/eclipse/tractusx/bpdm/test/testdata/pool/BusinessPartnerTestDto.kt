/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.test.testdata.pool

import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateVerboseResponse

data class LegalEntityStructureRequest(
    val legalEntity: LegalEntityPartnerCreateRequest,
    val siteStructures: List<SiteStructureRequest> = emptyList(),
    val addresses: List<AddressPartnerCreateRequest> = emptyList()
)

data class SiteStructureRequest(
    val site: SitePartnerCreateRequest,
    val addresses: List<AddressPartnerCreateRequest> = emptyList()
)

data class LegalEntityStructureResponse(
    val legalEntity: LegalEntityPartnerCreateVerboseResponse,
    val siteStructures: List<SiteStructureResponse> = emptyList(),
    val addresses: List<AddressPartnerCreateVerboseResponse> = emptyList()
)

data class SiteStructureResponse(
    val site: SitePartnerCreateVerboseResponse,
    val addresses: List<AddressPartnerCreateVerboseResponse> = emptyList()
)