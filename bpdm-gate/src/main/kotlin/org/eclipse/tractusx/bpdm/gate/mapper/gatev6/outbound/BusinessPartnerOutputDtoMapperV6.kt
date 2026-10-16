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
package org.eclipse.tractusx.bpdm.gate.mapper.gatev6.outbound

import org.eclipse.tractusx.bpdm.gate.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.AddressComponentOutputDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.LegalEntityRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SiteRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound.BusinessPartnerValueDtoMapper
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.BusinessPartnerOutputRecord
import org.springframework.stereotype.Component

/**
 * Maps a business partner output record to the output response of the v6 API.
 */
@Component
class BusinessPartnerOutputDtoMapperV6(
    private val valueDtoMapper: BusinessPartnerValueDtoMapper
) {

    /**
     * Returns the v6 API response for [record].
     */
    fun toDto(record: BusinessPartnerOutputRecord): BusinessPartnerOutputDto =
        BusinessPartnerOutputDto(
            externalId = record.externalId,
            nameParts = record.nameParts,
            identifiers = record.identifiers.map(valueDtoMapper::toDto),
            states = record.states.map(valueDtoMapper::toDto),
            roles = record.roles,
            isOwnCompanyData = record.isOwnCompanyData,
            legalEntity = with(record.legalEntity) {
                LegalEntityRepresentationOutputDto(
                    legalEntityBpn = bpn,
                    legalName = legalName,
                    shortName = shortName,
                    legalForm = legalForm,
                    confidenceCriteria = valueDtoMapper.toDto(confidence),
                    states = states.map(valueDtoMapper::toDto)
                )
            },
            site = record.site?.let { site ->
                SiteRepresentationOutputDto(
                    siteBpn = site.bpn,
                    name = site.name,
                    confidenceCriteria = valueDtoMapper.toDto(site.confidence),
                    states = site.states.map(valueDtoMapper::toDto)
                )
            },
            address = with(record.address) {
                AddressComponentOutputDto(
                    addressBpn = bpn,
                    name = name,
                    addressType = addressType,
                    physicalPostalAddress = physicalPostalAddress?.let(valueDtoMapper::toDto) ?: PhysicalPostalAddressDto(),
                    alternativePostalAddress = alternativePostalAddress?.let(valueDtoMapper::toDto) ?: AlternativePostalAddressDto(),
                    confidenceCriteria = valueDtoMapper.toDto(confidence),
                    states = states.map(valueDtoMapper::toDto)
                )
            },
            externalSequenceTimestamp = record.externalSequenceTimestamp,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
}
