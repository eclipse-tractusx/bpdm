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

package org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.BusinessPartnerIdentifierV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.BusinessPartnerScriptVariantV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.BusinessPartnerStateV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.IBaseBusinessPartnerGateV6Dto
import java.time.Instant

@Schema(
    description = "Generic business partner with external id",
    requiredProperties = ["externalId"]
)
data class BusinessPartnerInputV6Request(

    override val externalId: String,
    override val nameParts: List<String> = emptyList(),
    override val identifiers: Collection<BusinessPartnerIdentifierV6Dto> = emptyList(),
    override val states: Collection<BusinessPartnerStateV6Dto> = emptyList(),
    override val roles: Collection<BusinessPartnerRole> = emptyList(),
    override val isOwnCompanyData: Boolean = false,
    override val legalEntity: LegalEntityRepresentationInputV6Dto = LegalEntityRepresentationInputV6Dto(),
    override val site: SiteRepresentationInputV6Dto = SiteRepresentationInputV6Dto(),
    override val address: AddressRepresentationInputV6Dto = AddressRepresentationInputV6Dto(),
    override val externalSequenceTimestamp: Instant? = null,
    val scriptVariants: List<BusinessPartnerScriptVariantV6Dto> = emptyList(),

    @get:Schema(description = AdditionalSiteInputV6Dto.collectionDescription)
    val additionalSites: List<AdditionalSiteInputV6Dto> = emptyList()
) : IBaseBusinessPartnerGateV6Dto
