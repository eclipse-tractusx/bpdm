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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.*
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input.*
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.output.AddressComponentOutputV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.output.BusinessPartnerOutputV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.output.LegalEntityRepresentationOutputV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.output.SiteRepresentationOutputV6Dto
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.BusinessPartnerInputRecord
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.BusinessPartnerOutputRecord
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvRowError
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvUpsertError
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertError.AdditionalSitesWithoutOwnSite
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertError.DuplicateExternalIds
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertError.UpsertLimitExceeded
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw
import org.springframework.stereotype.Component

/**
 * Maps business partner records and errors to their API v6 form.
 */
@Component
class ApiV6BusinessPartnerMapper {

    /**
     * Returns the API response for [record].
     */
    fun toDto(record: BusinessPartnerInputRecord): BusinessPartnerInputV6Dto {
        val content = record.content
        return BusinessPartnerInputV6Dto(
            externalId = record.externalId,
            nameParts = content.nameParts,
            identifiers = content.identifiers.map { BusinessPartnerIdentifierV6Dto(type = it.type, value = it.value, issuingBody = it.issuingBody) },
            states = toStateDtos(content.states, BusinessPartnerType.GENERIC),
            roles = content.roles,
            isOwnCompanyData = content.isOwnCompanyData,
            legalEntity = with(content.legalEntity) {
                LegalEntityRepresentationInputV6Dto(
                    legalEntityBpn = bpn,
                    legalName = legalName,
                    shortName = shortName,
                    legalForm = legalForm,
                    ownershipUltimate = ownershipUltimate,
                    ultimateOwnerBpnl = ultimateOwnerBpnl,
                    states = toStateDtos(content.states, BusinessPartnerType.LEGAL_ENTITY)
                )
            },
            site = SiteRepresentationInputV6Dto(
                siteBpn = content.site.bpn,
                name = content.site.name,
                states = toStateDtos(content.states, BusinessPartnerType.SITE)
            ),
            address = with(content.address) {
                AddressRepresentationInputV6Dto(
                    addressBpn = bpn,
                    name = name,
                    addressType = addressType,
                    physicalPostalAddress = physicalPostalAddress?.let(::toDto) ?: PhysicalPostalAddressV6Dto(),
                    alternativePostalAddress = alternativePostalAddress?.let(::toDto),
                    states = toStateDtos(content.states, BusinessPartnerType.ADDRESS)
                )
            },
            externalSequenceTimestamp = record.externalSequenceTimestamp,
            scriptVariants = content.scriptVariants.map(::toDto),
            additionalSites = content.additionalSites.map { AdditionalSiteInputV6Dto(siteBpn = it.bpn, name = it.name) },
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
    }

    private fun toStateDtos(states: Collection<BusinessPartnerInputContentParsed.State>, businessPartnerType: BusinessPartnerType) =
        states.filter { it.businessPartnerType == businessPartnerType }
            .map { BusinessPartnerStateV6Dto(type = it.type, validFrom = it.validFrom, validTo = it.validTo) }

    /**
     * Returns the API response for [record].
     */
    fun toDto(record: BusinessPartnerOutputRecord): BusinessPartnerOutputV6Dto =
        BusinessPartnerOutputV6Dto(
            externalId = record.externalId,
            nameParts = record.nameParts,
            identifiers = record.identifiers.map(::toDto),
            states = record.states.map(::toDto),
            roles = record.roles,
            isOwnCompanyData = record.isOwnCompanyData,
            legalEntity = toDto(record.legalEntity),
            site = record.site?.let(::toDto),
            address = toDto(record.address),
            externalSequenceTimestamp = record.externalSequenceTimestamp,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )

    private fun toDto(legalEntity: BusinessPartnerOutputRecord.LegalEntity) =
        LegalEntityRepresentationOutputV6Dto(
            legalEntityBpn = legalEntity.bpn,
            legalName = legalEntity.legalName,
            shortName = legalEntity.shortName,
            legalForm = legalEntity.legalForm,
            confidenceCriteria = toDto(legalEntity.confidence),
            states = legalEntity.states.map(::toDto)
        )

    private fun toDto(site: BusinessPartnerOutputRecord.Site) =
        SiteRepresentationOutputV6Dto(
            siteBpn = site.bpn,
            name = site.name,
            confidenceCriteria = toDto(site.confidence),
            states = site.states.map(::toDto)
        )

    private fun toDto(address: BusinessPartnerOutputRecord.Address) =
        AddressComponentOutputV6Dto(
            addressBpn = address.bpn,
            name = address.name,
            addressType = address.addressType,
            physicalPostalAddress = address.physicalPostalAddress?.let(::toDto) ?: PhysicalPostalAddressV6Dto(),
            alternativePostalAddress = address.alternativePostalAddress?.let(::toDto) ?: AlternativePostalAddressV6Dto(),
            confidenceCriteria = toDto(address.confidence),
            states = address.states.map(::toDto)
        )

    private fun toDto(identifier: IdentifierRaw) =
        BusinessPartnerIdentifierV6Dto(type = identifier.type, value = identifier.value, issuingBody = identifier.issuingBody)

    private fun toDto(state: StateRaw) =
        BusinessPartnerStateV6Dto(type = state.type, validFrom = state.validFrom, validTo = state.validTo)

    private fun toDto(confidence: ConfidenceCriteriaRaw) =
        ConfidenceCriteriaV6Dto(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            numberOfSharingMembers = confidence.numberOfSharingMembers,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt,
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt,
            confidenceLevel = confidence.confidenceLevel
        )

    private fun toDto(address: BusinessPartnerValuesRaw.PhysicalPostalAddress) =
        PhysicalPostalAddressV6Dto(
            geographicCoordinates = address.geographicCoordinates?.let(::toDto),
            country = address.country,
            administrativeAreaLevel1 = address.administrativeAreaLevel1,
            administrativeAreaLevel2 = address.administrativeAreaLevel2,
            administrativeAreaLevel3 = address.administrativeAreaLevel3,
            postalCode = address.postalCode,
            city = address.city,
            district = address.district,
            street = address.street?.let(::toDto),
            companyPostalCode = address.companyPostalCode,
            industrialZone = address.industrialZone,
            building = address.building,
            floor = address.floor,
            door = address.door,
            taxJurisdictionCode = address.taxJurisdictionCode
        )

    private fun toDto(address: BusinessPartnerValuesRaw.AlternativePostalAddress) =
        AlternativePostalAddressV6Dto(
            geographicCoordinates = address.geographicCoordinates?.let(::toDto),
            country = address.country,
            administrativeAreaLevel1 = address.administrativeAreaLevel1,
            postalCode = address.postalCode,
            city = address.city,
            deliveryServiceType = address.deliveryServiceType,
            deliveryServiceQualifier = address.deliveryServiceQualifier,
            deliveryServiceNumber = address.deliveryServiceNumber
        )

    private fun toDto(street: StreetRaw) =
        StreetV6Dto(
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            name = street.name,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix,
            houseNumber = street.houseNumber,
            houseNumberSupplement = street.houseNumberSupplement,
            milestone = street.milestone,
            direction = street.direction
        )

    private fun toDto(coordinate: GeoCoordinateRaw) =
        GeoCoordinateDto(longitude = coordinate.longitude, latitude = coordinate.latitude, altitude = coordinate.altitude)

    private fun toDto(scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        BusinessPartnerScriptVariantV6Dto(
            scriptCode = scriptVariant.scriptCode,
            nameParts = scriptVariant.nameParts,
            legalEntity = LegalEntityScriptVariantV6Dto(legalName = scriptVariant.legalName, shortName = scriptVariant.shortName),
            site = SiteScriptVariantV6Dto(name = scriptVariant.siteName),
            address = AddressScriptVariantV6Dto(
                name = scriptVariant.addressName,
                physicalAddress = with(scriptVariant.physicalAddress) {
                    PhysicalAddressScriptVariantV6Dto(
                        city = city,
                        district = district,
                        street = with(street) {
                            StreetScriptVariantV6Dto(
                                name = name,
                                direction = direction,
                                namePrefix = namePrefix,
                                additionalNamePrefix = additionalNamePrefix,
                                nameSuffix = nameSuffix,
                                additionalNameSuffix = additionalNameSuffix
                            )
                        },
                        industrialZone = industrialZone,
                        building = building,
                        floor = floor,
                        door = door
                    )
                },
                alternativeAddress = scriptVariant.alternativeAddressCity?.let { AlternativeAddressScriptVariantV6Dto(city = it) }
            )
        )

    /**
     * Returns the message the API reports for [error].
     */
    fun toMessage(error: BusinessPartnerInputUpsertError): String =
        when (error) {
            is UpsertLimitExceeded ->
                "At most ${error.limit} business partners may be upserted at once, but ${error.actual} were given"
            is DuplicateExternalIds ->
                "External IDs occur more than once in the batch: ${error.externalIds.joinToString()}"
            is AdditionalSitesWithoutOwnSite ->
                "Partner with ID '${error.externalId}' is invalid: additional sites of its address are stated " +
                        "but no site of its own is, which they would be additional to"
        }

    /**
     * Returns the message the API reports for [error].
     */
    fun toMessage(error: BusinessPartnerCsvUpsertError): String =
        when (error) {
            is BusinessPartnerCsvUpsertError.InvalidTenantBpnl ->
                "The uploading tenant '${error.tenantBpnl.orEmpty()}' is not identified by a legal entity BPN"
            is BusinessPartnerCsvUpsertError.TenantLegalEntityNotFound ->
                "No legal entity with BPN '${error.tenantBpnl}' of the uploading tenant found in the Pool"
            is BusinessPartnerCsvUpsertError.InvalidRow ->
                "Row - ${error.rowNumber}${error.externalId?.let { ", External ID - $it" }.orEmpty()} has error: ${toProblem(error.error)}"
            is BusinessPartnerCsvUpsertError.InputRejected -> toMessage(error.error)
        }

    private fun toProblem(error: BusinessPartnerCsvRowError): String =
        when (error) {
            is BusinessPartnerCsvRowError.MissingExternalId -> "Column 'externalId' is missing and can not be empty or blank."
            is BusinessPartnerCsvRowError.InvalidDate -> "Invalid date format in field '${error.column}'."
            is BusinessPartnerCsvRowError.InvalidEnumValue -> "Invalid enum value in field '${error.column}'."
            is BusinessPartnerCsvRowError.InvalidNumber -> "Invalid or missing number in field '${error.column}'."
            is BusinessPartnerCsvRowError.UnsupportedAddressType ->
                "Invalid address type '${error.addressType}'. Only 'SiteMainAddress' and 'AdditionalAddress' are allowed."
        }
}
