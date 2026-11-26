/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.IBaseAddressRepresentation
import org.eclipse.tractusx.bpdm.common.dto.IBaseLegalEntityRepresentation
import org.eclipse.tractusx.bpdm.common.dto.IBaseSiteRepresentation
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.*
import java.time.Instant

data class BusinessPartnerOutputV7Request(
    override val externalId: String,

    @get:Schema(description = "The alternative refinement process this output belongs to. Outputs can only be written for alternative processes; the golden record process writes outputs itself.")
    val process: String,
    override val nameParts: List<String> = emptyList(),
    override val identifiers: Collection<BusinessPartnerIdentifierV7Dto> = emptyList(),
    override val states: Collection<BusinessPartnerStateOutputV7Dto> = emptyList(),
    override val roles: Collection<BusinessPartnerRole> = emptyList(),
    override val isOwnCompanyData: Boolean = false,
    override val legalEntity: LegalEntityRepresentationOutputV7Request = LegalEntityRepresentationOutputV7Request(),
    override val site: SiteRepresentationOutputV7Request? = null,
    override val address: AddressComponentOutputV7Request = AddressComponentOutputV7Request(),
    override val externalSequenceTimestamp: Instant? = null,
    val scriptVariants: List<BusinessPartnerScriptVariantV7Dto> = emptyList(),
    val additionalSites: Collection<AdditionalSiteOutputV7Dto> = emptyList()
) : IBaseBusinessPartnerGateV7Dto

data class LegalEntityRepresentationOutputV7Request(
    override val legalEntityBpn: String? = null,
    override val legalName: String? = null,
    override val shortName: String? = null,
    override val legalForm: String? = null,
    val confidenceCriteria: ConfidenceCriteriaV7Dto? = null,
    @get:Schema(description = "Designates whether this legal entity is the ultimate owner in an ownership chain.")
    val ownershipUltimate: Boolean? = null,
    @get:Schema(description = "The BPNL of the designated ultimate owner up in the ownership chain.")
    val ultimateOwnerBpnl: String? = null,
    override val states: Collection<BusinessPartnerStateOutputV7Dto> = emptyList(),
    val updatedAt: Instant? = null
) : IBaseLegalEntityRepresentation

data class SiteRepresentationOutputV7Request(
    override val siteBpn: String? = null,
    override val name: String? = null,
    val confidenceCriteria: ConfidenceCriteriaV7Dto? = null,
    override val states: Collection<BusinessPartnerStateOutputV7Dto> = emptyList(),
    val updatedAt: Instant? = null
) : IBaseSiteRepresentation

data class AddressComponentOutputV7Request(
    override val addressBpn: String? = null,
    override val name: String? = null,
    override val addressType: AddressType? = null,
    override val physicalPostalAddress: PhysicalPostalAddressV7Dto = PhysicalPostalAddressV7Dto(),
    override val alternativePostalAddress: AlternativePostalAddressV7Dto? = null,
    val confidenceCriteria: ConfidenceCriteriaV7Dto? = null,
    override val states: Collection<BusinessPartnerStateOutputV7Dto> = emptyList(),
    val identifiers: Collection<AddressIdentifierV7Dto> = emptyList(),
    val updatedAt: Instant? = null
) : IBaseAddressRepresentation
