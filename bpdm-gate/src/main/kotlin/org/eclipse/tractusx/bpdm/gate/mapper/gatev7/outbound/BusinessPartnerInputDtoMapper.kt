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

package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputContent
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputRecord
import org.springframework.stereotype.Component

/**
 * Maps a business partner input record to the input response of the API.
 */
@Component
class BusinessPartnerInputDtoMapper(
    private val valueDtoMapper: BusinessPartnerValueDtoMapper
) {

    /**
     * Returns the API response for [record].
     */
    fun toDto(record: BusinessPartnerInputRecord): BusinessPartnerInputDto {
        val content = record.content
        return BusinessPartnerInputDto(
            externalId = record.externalId,
            nameParts = content.nameParts,
            identifiers = content.identifiers.map { BusinessPartnerIdentifierDto(type = it.type, value = it.value, issuingBody = it.issuingBody) },
            states = toStateDtos(content.states, BusinessPartnerType.GENERIC),
            roles = content.roles,
            isOwnCompanyData = content.isOwnCompanyData,
            legalEntity = with(content.legalEntity) {
                LegalEntityRepresentationInputDto(
                    legalEntityBpn = bpn,
                    legalName = legalName,
                    shortName = shortName,
                    legalForm = legalForm,
                    ownershipUltimate = ownershipUltimate,
                    ultimateOwnerBpnl = ultimateOwnerBpnl,
                    states = toStateDtos(content.states, BusinessPartnerType.LEGAL_ENTITY)
                )
            },
            site = SiteRepresentationInputDto(
                siteBpn = content.site.bpn,
                name = content.site.name,
                states = toStateDtos(content.states, BusinessPartnerType.SITE)
            ),
            address = with(content.address) {
                AddressRepresentationInputDto(
                    addressBpn = bpn,
                    name = name,
                    addressType = addressType,
                    physicalPostalAddress = physicalPostalAddress?.let(valueDtoMapper::toDto) ?: PhysicalPostalAddressDto(),
                    alternativePostalAddress = alternativePostalAddress?.let(valueDtoMapper::toDto),
                    states = toStateDtos(content.states, BusinessPartnerType.ADDRESS)
                )
            },
            externalSequenceTimestamp = record.externalSequenceTimestamp,
            scriptVariants = content.scriptVariants.map(valueDtoMapper::toDto),
            additionalSites = content.additionalSites.map { AdditionalSiteInputDto(siteBpn = it.bpn, name = it.name) },
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
    }

    private fun toStateDtos(states: Collection<BusinessPartnerInputContent.State>, businessPartnerType: BusinessPartnerType) =
        states.filter { it.businessPartnerType == businessPartnerType }
            .map { BusinessPartnerStateDto(type = it.type, validFrom = it.validFrom, validTo = it.validTo) }
}
