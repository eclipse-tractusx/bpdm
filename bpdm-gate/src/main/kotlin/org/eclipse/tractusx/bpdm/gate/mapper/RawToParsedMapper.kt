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

package org.eclipse.tractusx.bpdm.gate.mapper

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerInputUpsertRaw
import org.springframework.stereotype.Component

/**
 * Maps raw models a parser has normalized and validated to their parsed form.
 */
@Component
class RawToParsedMapper {

    /**
     * Returns the parsed form of the normalized, validated [request].
     */
    fun toParsed(request: BusinessPartnerInputUpsertRaw): BusinessPartnerInputUpsertParsed =
        BusinessPartnerInputUpsertParsed(
            externalId = request.externalId,
            externalSequenceTimestamp = request.externalSequenceTimestamp,
            content = toContent(request)
        )

    private fun toContent(request: BusinessPartnerInputUpsertRaw) =
        with(request) {
            BusinessPartnerInputContentParsed(
                nameParts = nameParts,
                identifiers = identifiers.mapTo(linkedSetOf(), ::toContent),
                states = toContent(states, BusinessPartnerType.GENERIC) +
                        toContent(legalEntity.states, BusinessPartnerType.LEGAL_ENTITY) +
                        toContent(site.states, BusinessPartnerType.SITE) +
                        toContent(address.states, BusinessPartnerType.ADDRESS),
                roles = roles.toCollection(linkedSetOf()),
                isOwnCompanyData = isOwnCompanyData,
                legalEntity = with(legalEntity) {
                    BusinessPartnerInputContentParsed.LegalEntity(bpn, legalName, shortName, legalForm, ownershipUltimate, ultimateOwnerBpnl)
                },
                site = BusinessPartnerInputContentParsed.Site(site.bpn, site.name),
                address = with(address) {
                    BusinessPartnerInputContentParsed.Address(bpn, name, addressType, physicalPostalAddress, alternativePostalAddress)
                },
                scriptVariants = scriptVariants.toCollection(linkedSetOf()),
                additionalSites = additionalSites.toCollection(linkedSetOf())
            )
        }

    // The normalizer has dropped every identifier and state missing these fields.
    private fun toContent(identifier: BusinessPartnerInputUpsertRaw.Identifier) =
        BusinessPartnerInputContentParsed.Identifier(type = identifier.type!!, value = identifier.value!!, issuingBody = identifier.issuingBody)

    private fun toContent(states: List<BusinessPartnerInputUpsertRaw.State>, businessPartnerType: BusinessPartnerType) =
        states.mapTo(linkedSetOf()) {
            BusinessPartnerInputContentParsed.State(type = it.type!!, validFrom = it.validFrom, validTo = it.validTo, businessPartnerType = businessPartnerType)
        }
}
