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

package org.eclipse.tractusx.bpdm.gate.mapper.parsed

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputContent
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertIn
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertParsed
import org.springframework.stereotype.Component

/**
 * Maps a normalized, validated business partner input upsert entry to its parsed form.
 */
@Component
class BusinessPartnerInputUpsertParsedMapper {

    /**
     * Returns the parsed form of the normalized, validated [request].
     */
    fun toParsed(request: BusinessPartnerInputUpsertIn): BusinessPartnerInputUpsertParsed =
        BusinessPartnerInputUpsertParsed(
            externalId = request.externalId,
            externalSequenceTimestamp = request.externalSequenceTimestamp,
            content = toContent(request)
        )

    private fun toContent(request: BusinessPartnerInputUpsertIn) =
        with(request) {
            BusinessPartnerInputContent(
                nameParts = nameParts,
                identifiers = identifiers.mapTo(linkedSetOf(), ::toContent),
                states = toContent(states, BusinessPartnerType.GENERIC) +
                        toContent(legalEntity.states, BusinessPartnerType.LEGAL_ENTITY) +
                        toContent(site.states, BusinessPartnerType.SITE) +
                        toContent(address.states, BusinessPartnerType.ADDRESS),
                roles = roles.toCollection(linkedSetOf()),
                isOwnCompanyData = isOwnCompanyData,
                legalEntity = with(legalEntity) {
                    BusinessPartnerInputContent.LegalEntity(bpn, legalName, shortName, legalForm, ownershipUltimate, ultimateOwnerBpnl)
                },
                site = BusinessPartnerInputContent.Site(site.bpn, site.name),
                address = with(address) {
                    BusinessPartnerInputContent.Address(bpn, name, addressType, physicalPostalAddress, alternativePostalAddress)
                },
                scriptVariants = scriptVariants.toCollection(linkedSetOf()),
                additionalSites = additionalSites.toCollection(linkedSetOf())
            )
        }

    // The normalizer has dropped every identifier and state missing these fields.
    private fun toContent(identifier: BusinessPartnerInputUpsertIn.Identifier) =
        BusinessPartnerInputContent.Identifier(type = identifier.type!!, value = identifier.value!!, issuingBody = identifier.issuingBody)

    private fun toContent(states: List<BusinessPartnerInputUpsertIn.State>, businessPartnerType: BusinessPartnerType) =
        states.mapTo(linkedSetOf()) {
            BusinessPartnerInputContent.State(type = it.type!!, validFrom = it.validFrom, validTo = it.validTo, businessPartnerType = businessPartnerType)
        }
}
