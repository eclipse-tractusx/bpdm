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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv6

import org.eclipse.tractusx.bpdm.gate.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.AddressComponentOutputDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.LegalEntityRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SiteRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationAlreadyExistsException
import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationInputDeleteException
import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationInputUpsertException
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7BusinessPartnerMapper
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7RelationMapper
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7SharingStateMapper
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.BusinessPartnerOutputRecord
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationInputRecord
import org.eclipse.tractusx.bpdm.gate.model.output.sharingstate.SharingStateRecord
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputDeleteError
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputDeleteError.NotFound
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError
import org.springframework.stereotype.Component

/**
 * Maps records and errors to their API v6 form.
 */
@Component
class ApiV6OutboundMapper(
    private val apiV7BusinessPartnerMapper: ApiV7BusinessPartnerMapper,
    private val apiV7SharingStateMapper: ApiV7SharingStateMapper,
    private val apiV7RelationMapper: ApiV7RelationMapper
) {

    /**
     * Returns the v6 API response for [record].
     */
    fun toDto(record: BusinessPartnerOutputRecord): BusinessPartnerOutputDto =
        BusinessPartnerOutputDto(
            externalId = record.externalId,
            nameParts = record.nameParts,
            identifiers = record.identifiers.map(apiV7BusinessPartnerMapper::toDto),
            states = record.states.map(apiV7BusinessPartnerMapper::toDto),
            roles = record.roles,
            isOwnCompanyData = record.isOwnCompanyData,
            legalEntity = with(record.legalEntity) {
                LegalEntityRepresentationOutputDto(
                    legalEntityBpn = bpn,
                    legalName = legalName,
                    shortName = shortName,
                    legalForm = legalForm,
                    confidenceCriteria = apiV7BusinessPartnerMapper.toDto(confidence),
                    states = states.map(apiV7BusinessPartnerMapper::toDto)
                )
            },
            site = record.site?.let { site ->
                SiteRepresentationOutputDto(
                    siteBpn = site.bpn,
                    name = site.name,
                    confidenceCriteria = apiV7BusinessPartnerMapper.toDto(site.confidence),
                    states = site.states.map(apiV7BusinessPartnerMapper::toDto)
                )
            },
            address = with(record.address) {
                AddressComponentOutputDto(
                    addressBpn = bpn,
                    name = name,
                    addressType = addressType,
                    physicalPostalAddress = physicalPostalAddress?.let(apiV7BusinessPartnerMapper::toDto) ?: PhysicalPostalAddressDto(),
                    alternativePostalAddress = alternativePostalAddress?.let(apiV7BusinessPartnerMapper::toDto) ?: AlternativePostalAddressDto(),
                    confidenceCriteria = apiV7BusinessPartnerMapper.toDto(confidence),
                    states = states.map(apiV7BusinessPartnerMapper::toDto)
                )
            },
            externalSequenceTimestamp = record.externalSequenceTimestamp,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )

    /**
     * Returns the v6 API response for [record].
     */
    fun toDto(record: SharingStateRecord): SharingStateDto =
        SharingStateDto(
            externalId = record.externalId,
            sharingStateType = apiV7SharingStateMapper.toDto(record.sharingStateType),
            sharingErrorCode = record.sharingErrorCode?.let(apiV7SharingStateMapper::toDto),
            sharingErrorMessage = record.sharingErrorMessage,
            sharingProcessStarted = record.sharingProcessStarted,
            taskId = record.taskId
        )

    /**
     * Returns the v6 API relation for [record].
     */
    fun toDto(record: RelationInputRecord): RelationDto =
        RelationDto(
            externalId = record.externalId,
            relationType = apiV7RelationMapper.toDto(record.relationType),
            businessPartnerSourceExternalId = record.sourceExternalId,
            businessPartnerTargetExternalId = record.targetExternalId,
            updatedAt = record.updatedAt,
            createdAt = record.createdAt
        )

    /**
     * Returns the conflict for a relation that already exists if [errors] name one, and the bad request reporting every
     * error otherwise.
     */
    fun toUpsertException(errors: List<RelationInputUpsertError>): RuntimeException =
        errors.filterIsInstance<RelationInputUpsertError.AlreadyExists>().firstOrNull()
            ?.let { BpdmRelationAlreadyExistsException(it.externalId) }
            ?: BpdmRelationInputUpsertException(errors.map(apiV7RelationMapper::toMessage))

    /**
     * Returns the bad request reporting every error in [errors].
     */
    fun toDeleteException(errors: List<RelationInputDeleteError>): RuntimeException =
        BpdmRelationInputDeleteException(errors.map(::toMessage))

    private fun toMessage(error: RelationInputDeleteError): String =
        when (error) {
            is NotFound -> "Relation '${error.externalId}' can not be found."
        }
}
