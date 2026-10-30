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

package org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.CommonDescription
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.*
import java.time.Instant

@Schema(
    description = "Generic business partner output with external id",
    requiredProperties = ["externalId"]
)
data class BusinessPartnerOutputV7Dto(

    override val externalId: String,
    override val nameParts: List<String> = emptyList(),
    override val identifiers: Collection<BusinessPartnerIdentifierV7Dto> = emptyList(),
    override val states: Collection<BusinessPartnerStateV7Dto> = emptyList(),
    override val roles: Collection<BusinessPartnerRole> = emptyList(),
    override val isOwnCompanyData: Boolean = false,
    override val legalEntity: LegalEntityRepresentationOutputV7Dto,
    override val site: SiteRepresentationOutputV7Dto?,
    override val address: AddressComponentOutputV7Dto,
    override val externalSequenceTimestamp: Instant? = null,
    val scriptVariants: List<BusinessPartnerScriptVariantV7Dto> = emptyList(),

    @get:Schema(description = "The further sites this business partner's address belongs to, in addition to the primary 'site'.")
    val additionalSites: Collection<AdditionalSiteOutputV7Dto> = emptyList(),

    @get:Schema(description = CommonDescription.createdAt)
    val createdAt: Instant,

    @get:Schema(description = "Timestamp when the business partner record was last updated")
    val updatedAt: Instant

) : IBaseBusinessPartnerGateV7Dto

@Schema(
    description = "Legal Entity properties of business partner output data",
    requiredProperties = ["bpnL"]
)
data class LegalEntityRepresentationOutputV7Dto(
    override val legalEntityBpn: String,
    override val legalName: String? = null,
    override val shortName: String? = null,
    override val legalForm: String? = null,
    val confidenceCriteria: ConfidenceCriteriaV7Dto,
    @get:Schema(description = "Designates whether this legal entity is the ultimate owner in an ownership chain.")
    val ownershipUltimate: Boolean? = null,
    @get:Schema(description = "The BPNL of the designated ultimate owner up in the ownership chain.")
    val ultimateOwnerBpnl: String? = null,
    override val states: Collection<BusinessPartnerStateV7Dto> = emptyList(),
    val goldenRecordRelations: List<LegalEntityGoldenRecordRelationV7Dto> = emptyList(),

    @get:Schema(description = "Timestamp when the associated legal entity golden record was last updated")
    val updatedAt: Instant? = null
) : IBaseLegalEntityRepresentation

@Schema(
    description = "Site properties of business partner output data"
)
data class SiteRepresentationOutputV7Dto(
    override val siteBpn: String,
    override val name: String? = null,
    val confidenceCriteria: ConfidenceCriteriaV7Dto,
    override val states: Collection<BusinessPartnerStateV7Dto> = emptyList(),
    val goldenRecordRelations: List<SiteGoldenRecordRelationV7Dto> = emptyList(),

    @get:Schema(description = "Timestamp when the associated site golden record was last updated")
    val updatedAt: Instant? = null
) : IBaseSiteRepresentation

@Schema(
    description = "A further site a business partner's address belongs to, in addition to its primary site",
    requiredProperties = ["siteBpn"]
)
data class AdditionalSiteOutputV7Dto(
    @get:Schema(description = "The BPNS of the site")
    val siteBpn: String,
    @get:Schema(description = "The name of the site")
    val name: String? = null
)

@Schema(
    description = "Address properties of business partner output data",
    requiredProperties = ["bpnA"]
)
data class AddressComponentOutputV7Dto(
    override val addressBpn: String,
    override val name: String? = null,
    override val addressType: AddressType?,
    override val physicalPostalAddress: PhysicalPostalAddressV7Dto = PhysicalPostalAddressV7Dto(),
    override val alternativePostalAddress: AlternativePostalAddressV7Dto? = null,
    val confidenceCriteria: ConfidenceCriteriaV7Dto,
    override val states: Collection<BusinessPartnerStateV7Dto> = emptyList(),
    val identifiers: Collection<AddressIdentifierV7Dto> = emptyList(),
    val goldenRecordRelations: List<AddressGoldenRecordRelationV7Dto> = emptyList(),

    @get:Schema(description = "Timestamp when the associated address golden record was last updated")
    val updatedAt: Instant? = null
) : IBaseAddressRepresentation
