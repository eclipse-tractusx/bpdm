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

package org.eclipse.tractusx.bpdm.gate.api.v7.model.request

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.gate.api.v7.model.BusinessPartnerIdentifierV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.BusinessPartnerScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.BusinessPartnerStateV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.IBaseBusinessPartnerGateV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.AdditionalSiteInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.AddressRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.LegalEntityRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.SiteRepresentationInputV7Dto
import java.time.Instant

@Schema(
    description = "Generic business partner with external id",
    requiredProperties = ["externalId"]
)
data class BusinessPartnerInputV7Request(

    override val externalId: String,
    override val nameParts: List<String> = emptyList(),
    override val identifiers: Collection<BusinessPartnerIdentifierV7Dto> = emptyList(),
    override val states: Collection<BusinessPartnerStateV7Dto> = emptyList(),
    override val roles: Collection<BusinessPartnerRole> = emptyList(),
    override val isOwnCompanyData: Boolean = false,
    override val legalEntity: LegalEntityRepresentationInputV7Dto = LegalEntityRepresentationInputV7Dto(),
    override val site: SiteRepresentationInputV7Dto = SiteRepresentationInputV7Dto(),
    override val address: AddressRepresentationInputV7Dto = AddressRepresentationInputV7Dto(),
    override val externalSequenceTimestamp: Instant? = null,
    val scriptVariants: List<BusinessPartnerScriptVariantV7Dto> = emptyList(),

    @get:Schema(description = AdditionalSiteInputV7Dto.collectionDescription)
    val additionalSites: List<AdditionalSiteInputV7Dto> = emptyList()
) : IBaseBusinessPartnerGateV7Dto
