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

package org.eclipse.tractusx.bpdm.pool.util

import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateVerboseDto

data class LegalEntityStructureRequest(
    val legalEntity: LegalEntityPartnerCreateDto,
    val siteStructures: List<SiteStructureRequest> = emptyList(),
    val addresses: List<AddressPartnerCreateDto> = emptyList()
)

data class SiteStructureRequest(
    val site: SitePartnerCreateDto,
    val addresses: List<AddressPartnerCreateDto> = emptyList()
)

data class LegalEntityStructureResponse(
    val legalEntity: LegalEntityPartnerCreateVerboseDto,
    val siteStructures: List<SiteStructureResponse> = emptyList(),
    val addresses: List<AddressPartnerCreateVerboseDto> = emptyList()
)

data class SiteStructureResponse(
    val site: SitePartnerCreateVerboseDto,
    val addresses: List<AddressPartnerCreateVerboseDto> = emptyList()
)