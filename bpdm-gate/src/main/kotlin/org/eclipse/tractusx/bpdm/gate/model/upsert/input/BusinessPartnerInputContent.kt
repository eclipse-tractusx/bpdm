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

package org.eclipse.tractusx.bpdm.gate.model.upsert.input

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.gate.model.value.BusinessPartnerValues
import java.time.LocalDateTime

/**
 * Everything a business partner input stores about the partner, so two equal contents mean no change.
 */
data class BusinessPartnerInputContent(
    val nameParts: List<String>,
    val identifiers: Set<Identifier>,
    val states: Set<State>,
    val roles: Set<BusinessPartnerRole>,
    val isOwnCompanyData: Boolean,
    val legalEntity: LegalEntity,
    val site: Site,
    val address: Address,
    val scriptVariants: Set<BusinessPartnerValues.ScriptVariant>,
    val additionalSites: Set<BusinessPartnerValues.AdditionalSite>
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
        val physicalPostalAddress: BusinessPartnerValues.PhysicalPostalAddress?,
        val alternativePostalAddress: BusinessPartnerValues.AlternativePostalAddress?
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
