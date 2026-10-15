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

package org.eclipse.tractusx.bpdm.gate.service.businesspartner.input.parser

import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertIn
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputValues
import org.eclipse.tractusx.bpdm.gate.model.value.Street
import org.springframework.stereotype.Component

/**
 * Brings a business partner input upsert entry into the canonical form every validator and the parsed model rely on.
 *
 * Normalization never rejects: content that carries no information, such as an identifier without a value or an
 * all-empty postal address, is dropped rather than reported.
 */
@Component
class BusinessPartnerInputUpsertNormalizer {

    /**
     * Returns [request] in canonical form.
     */
    fun normalize(request: BusinessPartnerInputUpsertIn): BusinessPartnerInputUpsertIn =
        request.copy(
            identifiers = request.identifiers.filter { it.type != null && it.value != null },
            states = normalize(request.states),
            roles = request.roles.distinct(),
            legalEntity = request.legalEntity.copy(states = normalize(request.legalEntity.states)),
            site = request.site.copy(states = normalize(request.site.states)),
            address = normalize(request.address)
        )

    private fun normalize(states: List<BusinessPartnerInputUpsertIn.State>) =
        states.filter { it.type != null }

    private fun normalize(address: BusinessPartnerInputUpsertIn.Address) =
        address.copy(
            physicalPostalAddress = address.physicalPostalAddress
                ?.let { it.copy(street = it.street?.takeIf { street -> street != EMPTY_STREET }) }
                ?.takeIf { it != EMPTY_PHYSICAL_ADDRESS },
            alternativePostalAddress = address.alternativePostalAddress?.takeIf { it != EMPTY_ALTERNATIVE_ADDRESS },
            states = normalize(address.states)
        )

    private companion object {
        val EMPTY_STREET = Street(
            namePrefix = null, additionalNamePrefix = null, name = null, nameSuffix = null, additionalNameSuffix = null,
            houseNumber = null, houseNumberSupplement = null, milestone = null, direction = null
        )
        val EMPTY_PHYSICAL_ADDRESS = BusinessPartnerInputValues.PhysicalPostalAddress(
            geographicCoordinates = null, country = null, administrativeAreaLevel1 = null, administrativeAreaLevel2 = null,
            administrativeAreaLevel3 = null, postalCode = null, city = null, district = null, street = null,
            companyPostalCode = null, industrialZone = null, building = null, floor = null, door = null,
            taxJurisdictionCode = null
        )
        val EMPTY_ALTERNATIVE_ADDRESS = BusinessPartnerInputValues.AlternativePostalAddress(
            geographicCoordinates = null, country = null, administrativeAreaLevel1 = null, postalCode = null, city = null,
            deliveryServiceType = null, deliveryServiceQualifier = null, deliveryServiceNumber = null
        )
    }
}
