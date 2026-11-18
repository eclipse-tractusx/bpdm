/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
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
    override val legalEntity: LegalEntityRepresentationOutputV7Dto = LegalEntityRepresentationOutputV7Dto(),
    override val site: SiteRepresentationOutputV7Dto? = null,
    override val address: AddressComponentOutputV7Dto = AddressComponentOutputV7Dto(),
    override val externalSequenceTimestamp: Instant? = null,
    val scriptVariants: List<BusinessPartnerScriptVariantV7Dto> = emptyList(),
    val additionalSites: Collection<AdditionalSiteOutputV7Dto> = emptyList()
) : IBaseBusinessPartnerGateV7Dto
