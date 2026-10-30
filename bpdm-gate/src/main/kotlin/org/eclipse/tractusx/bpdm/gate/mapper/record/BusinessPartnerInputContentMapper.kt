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

package org.eclipse.tractusx.bpdm.gate.mapper.record

import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.StateDb
import org.eclipse.tractusx.bpdm.gate.model.raw.value.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputContent
import org.springframework.stereotype.Component

/**
 * Maps a stored business partner input to the content an upsert request would need to leave it unchanged.
 */
@Component
class BusinessPartnerInputContentMapper(
    private val valueMapper: BusinessPartnerValueMapper
) {

    /**
     * Returns the content of [entity].
     */
    fun toContent(entity: BusinessPartnerDb): BusinessPartnerInputContent =
        BusinessPartnerInputContent(
            nameParts = entity.nameParts.toList(),
            identifiers = entity.identifiers.mapTo(linkedSetOf()) { BusinessPartnerInputContent.Identifier(it.type, it.value, it.issuingBody) },
            states = entity.states.mapTo(linkedSetOf(), ::toContent),
            roles = entity.roles.toCollection(linkedSetOf()),
            isOwnCompanyData = entity.isOwnCompanyData,
            legalEntity = BusinessPartnerInputContent.LegalEntity(
                bpn = entity.bpnL,
                legalName = entity.legalName,
                shortName = entity.shortName,
                legalForm = entity.legalForm,
                ownershipUltimate = entity.ownershipUltimate,
                ultimateOwnerBpnl = entity.ultimateOwnerBpnl
            ),
            site = BusinessPartnerInputContent.Site(bpn = entity.bpnS, name = entity.siteName),
            address = BusinessPartnerInputContent.Address(
                bpn = entity.bpnA,
                name = entity.addressName,
                addressType = entity.postalAddress.addressType,
                physicalPostalAddress = entity.postalAddress.physicalPostalAddress?.let(valueMapper::toValue),
                alternativePostalAddress = entity.postalAddress.alternativePostalAddress?.let(valueMapper::toValue)
            ),
            scriptVariants = entity.scriptVariants.mapTo(linkedSetOf(), valueMapper::toValue),
            additionalSites = entity.additionalSites.mapTo(linkedSetOf()) { BusinessPartnerValuesRaw.AdditionalSite(bpn = it.bpn, name = it.name) }
        )

    private fun toContent(state: StateDb) =
        BusinessPartnerInputContent.State(type = state.type, validFrom = state.validFrom, validTo = state.validTo, businessPartnerType = state.businessPartnerTyp)
}
