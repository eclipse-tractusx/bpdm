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
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import java.time.Instant
import java.time.LocalDateTime

data class BusinessPartnerInputUpsertIn(
    val externalId: String,
    val nameParts: List<String>,
    val identifiers: List<Identifier>,
    val states: List<State>,
    val roles: List<BusinessPartnerRole>,
    val isOwnCompanyData: Boolean,
    val legalEntity: LegalEntity,
    val site: Site,
    val address: Address,
    val externalSequenceTimestamp: Instant?,
    val scriptVariants: List<BusinessPartnerInputValues.ScriptVariant>,
    val additionalSites: List<BusinessPartnerInputValues.AdditionalSite>
) {
    data class LegalEntity(
        val bpn: String?,
        val legalName: String?,
        val shortName: String?,
        val legalForm: String?,
        val ownershipUltimate: Boolean?,
        val ultimateOwnerBpnl: String?,
        val states: List<State>
    )

    data class Site(
        val bpn: String?,
        val name: String?,
        val states: List<State>
    )

    data class Address(
        val bpn: String?,
        val name: String?,
        val addressType: AddressType?,
        val physicalPostalAddress: BusinessPartnerInputValues.PhysicalPostalAddress?,
        val alternativePostalAddress: BusinessPartnerInputValues.AlternativePostalAddress?,
        val states: List<State>
    )

    data class Identifier(
        val type: String?,
        val value: String?,
        val issuingBody: String?
    )

    data class State(
        val type: BusinessStateType?,
        val validFrom: LocalDateTime?,
        val validTo: LocalDateTime?
    )
}
