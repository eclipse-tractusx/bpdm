/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import java.time.LocalDateTime

/**
 * Everything a business partner input stores about the partner, so two equal contents mean no change.
 *
 * Guarantees:
 * - [additionalSites] is empty unless [site] states a BPN or a name.
 */
data class BusinessPartnerInputContentParsed(
    val nameParts: List<String>,
    val identifiers: Set<Identifier>,
    val states: Set<State>,
    val roles: Set<BusinessPartnerRole>,
    val isOwnCompanyData: Boolean,
    val legalEntity: LegalEntity,
    val site: Site,
    val address: Address,
    val scriptVariants: Set<BusinessPartnerValuesRaw.ScriptVariant>,
    val additionalSites: Set<BusinessPartnerValuesRaw.AdditionalSite>
) {
    data class LegalEntity(
        val bpn: String?,
        val legalName: String?,
        val shortName: String?,
        val legalForm: String?,
        val ownershipUltimate: Boolean?,
        val ultimateOwnerBpnl: String?
    )

    data class Site(
        val bpn: String?,
        val name: String?
    )

    data class Address(
        val bpn: String?,
        val name: String?,
        val addressType: AddressType?,
        val physicalPostalAddress: BusinessPartnerValuesRaw.PhysicalPostalAddress?,
        val alternativePostalAddress: BusinessPartnerValuesRaw.AlternativePostalAddress?
    )

    data class Identifier(
        val type: String,
        val value: String,
        val issuingBody: String?
    )

    data class State(
        val type: BusinessStateType,
        val validFrom: LocalDateTime?,
        val validTo: LocalDateTime?,
        val businessPartnerType: BusinessPartnerType
    )
}
