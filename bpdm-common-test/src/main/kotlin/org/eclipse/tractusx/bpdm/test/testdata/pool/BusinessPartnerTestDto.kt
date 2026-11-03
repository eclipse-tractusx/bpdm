/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateVerboseV7Dto


data class LegalEntityStructureRequest(
    val legalEntity: LegalEntityPartnerCreateV7Request,
    val siteStructures: List<SiteStructureRequest> = emptyList(),
    val addresses: List<AddressPartnerCreateV7Request> = emptyList()
)

data class SiteStructureRequest(
    val site: SitePartnerCreateV7Request,
    val addresses: List<AddressPartnerCreateV7Request> = emptyList()
)

data class LegalEntityStructureResponse(
    val legalEntity: LegalEntityWithLegalAddressVerboseV7Dto,
    val siteStructures: List<SiteStructureResponse> = emptyList(),
    val addresses: List<AddressPartnerCreateVerboseV7Dto> = emptyList()
)

data class SiteStructureResponse(
    val site: SitePartnerCreateVerboseV7Dto,
    val addresses: List<AddressPartnerCreateVerboseV7Dto> = emptyList()
)