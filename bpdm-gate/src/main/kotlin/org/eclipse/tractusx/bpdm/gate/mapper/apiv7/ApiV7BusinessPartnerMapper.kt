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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv7

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.*
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.*
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
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRelationType
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Maps business partner records and errors to their API v7 form.
 */
@Component
class ApiV7BusinessPartnerMapper {

    /**
     * Returns the API response for [record].
     */
    fun toDto(record: BusinessPartnerInputRecord): BusinessPartnerInputV7Dto {
        val content = record.content
        return BusinessPartnerInputV7Dto(
            externalId = record.externalId,
            nameParts = content.nameParts,
            identifiers = content.identifiers.map { BusinessPartnerIdentifierV7Dto(type = it.type, value = it.value, issuingBody = it.issuingBody) },
            states = toStateDtos(content.states, BusinessPartnerType.GENERIC),
            roles = content.roles,
            isOwnCompanyData = content.isOwnCompanyData,
            legalEntity = with(content.legalEntity) {
                LegalEntityRepresentationInputV7Dto(
                    legalEntityBpn = bpn,
                    legalName = legalName,
                    shortName = shortName,
                    legalForm = legalForm,
                    ownershipUltimate = ownershipUltimate,
                    ultimateOwnerBpnl = ultimateOwnerBpnl,
                    states = toStateDtos(content.states, BusinessPartnerType.LEGAL_ENTITY)
                )
            },
            site = SiteRepresentationInputV7Dto(
                siteBpn = content.site.bpn,
                name = content.site.name,
                states = toStateDtos(content.states, BusinessPartnerType.SITE)
            ),
            address = with(content.address) {
                AddressRepresentationInputV7Dto(
                    addressBpn = bpn,
                    name = name,
                    addressType = addressType,
                    physicalPostalAddress = physicalPostalAddress?.let(::toDto) ?: PhysicalPostalAddressV7Dto(),
                    alternativePostalAddress = alternativePostalAddress?.let(::toDto),
                    states = toStateDtos(content.states, BusinessPartnerType.ADDRESS)
                )
            },
            externalSequenceTimestamp = record.externalSequenceTimestamp,
            scriptVariants = content.scriptVariants.map(::toDto),
            additionalSites = content.additionalSites.map { AdditionalSiteInputV7Dto(siteBpn = it.bpn, name = it.name) },
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
    }

    private fun toStateDtos(states: Collection<BusinessPartnerInputContentParsed.State>, businessPartnerType: BusinessPartnerType) =
        states.filter { it.businessPartnerType == businessPartnerType }
            .map { BusinessPartnerStateV7Dto(type = it.type, validFrom = it.validFrom, validTo = it.validTo) }

    /**
     * Returns the API response for [record].
     */
    fun toDto(record: BusinessPartnerOutputRecord): BusinessPartnerOutputV7Dto =
        BusinessPartnerOutputV7Dto(
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
            scriptVariants = record.scriptVariants.map(::toDto),
            additionalSites = record.additionalSites.map { AdditionalSiteOutputV7Dto(siteBpn = it.siteBpn, name = it.name) },
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )

    private fun toDto(legalEntity: BusinessPartnerOutputRecord.LegalEntity) =
        LegalEntityRepresentationOutputV7Dto(
            legalEntityBpn = legalEntity.bpn,
            legalName = legalEntity.legalName,
            shortName = legalEntity.shortName,
            legalForm = legalEntity.legalForm,
            confidenceCriteria = toDto(legalEntity.confidence),
            ownershipUltimate = legalEntity.ownershipUltimate,
            ultimateOwnerBpnl = legalEntity.ultimateOwnerBpnl,
            states = legalEntity.states.map(::toDto),
            goldenRecordRelations = legalEntity.goldenRecordRelations.map { LegalEntityGoldenRecordRelationV7Dto(toDto(it.type), it.sourceBpn, it.targetBpn) },
            updatedAt = legalEntity.updatedAt ?: Instant.EPOCH
        )

    private fun toDto(site: BusinessPartnerOutputRecord.Site) =
        SiteRepresentationOutputV7Dto(
            siteBpn = site.bpn,
            name = site.name,
            confidenceCriteria = toDto(site.confidence),
            states = site.states.map(::toDto),
            goldenRecordRelations = site.goldenRecordRelations.map { SiteGoldenRecordRelationV7Dto(toDto(it.type), it.sourceBpn, it.targetBpn) },
            updatedAt = site.updatedAt ?: Instant.EPOCH
        )

    private fun toDto(address: BusinessPartnerOutputRecord.Address) =
        AddressComponentOutputV7Dto(
            addressBpn = address.bpn,
            name = address.name,
            addressType = address.addressType,
            physicalPostalAddress = address.physicalPostalAddress?.let(::toDto) ?: PhysicalPostalAddressV7Dto(),
            alternativePostalAddress = address.alternativePostalAddress?.let(::toDto) ?: AlternativePostalAddressV7Dto(),
            confidenceCriteria = toDto(address.confidence),
            states = address.states.map(::toDto),
            goldenRecordRelations = address.goldenRecordRelations.map { AddressGoldenRecordRelationV7Dto(toDto(it.type), it.sourceBpn, it.targetBpn) },
            updatedAt = address.updatedAt ?: Instant.EPOCH
        )

    private fun toDto(type: LegalEntityGoldenRecordRelationType) =
        when (type) {
            LegalEntityGoldenRecordRelationType.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationTypeV7Dto.IsAlternativeHeadquarterFor
            LegalEntityGoldenRecordRelationType.IsManagedBy -> LegalEntityGoldenRecordRelationTypeV7Dto.IsManagedBy
            LegalEntityGoldenRecordRelationType.IsOwnedBy -> LegalEntityGoldenRecordRelationTypeV7Dto.IsOwnedBy
            LegalEntityGoldenRecordRelationType.IsReplacedBy -> LegalEntityGoldenRecordRelationTypeV7Dto.IsReplacedBy
        }

    private fun toDto(type: SiteGoldenRecordRelationType) =
        when (type) {
            SiteGoldenRecordRelationType.IsReplacedBy -> SiteGoldenRecordRelationTypeV7Dto.IsReplacedBy
        }

    private fun toDto(type: AddressGoldenRecordRelationType) =
        when (type) {
            AddressGoldenRecordRelationType.IsReplacedBy -> AddressGoldenRecordRelationTypeV7Dto.IsReplacedBy
        }

    /**
     * Returns the API form of an identifier.
     */
    fun toDto(identifier: IdentifierRaw) =
        BusinessPartnerIdentifierV7Dto(type = identifier.type, value = identifier.value, issuingBody = identifier.issuingBody)

    /**
     * Returns the API form of a business state.
     */
    fun toDto(state: StateRaw) =
        BusinessPartnerStateV7Dto(type = state.type, validFrom = state.validFrom, validTo = state.validTo)

    /**
     * Returns the API form of confidence criteria.
     */
    fun toDto(confidence: ConfidenceCriteriaRaw) =
        ConfidenceCriteriaV7Dto(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            numberOfSharingMembers = confidence.numberOfSharingMembers,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt,
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt,
            confidenceLevel = confidence.confidenceLevel
        )

    /**
     * Returns the API form of a physical postal address.
     */
    fun toDto(address: BusinessPartnerValuesRaw.PhysicalPostalAddress) =
        PhysicalPostalAddressV7Dto(
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

    /**
     * Returns the API form of an alternative postal address.
     */
    fun toDto(address: BusinessPartnerValuesRaw.AlternativePostalAddress) =
        AlternativePostalAddressV7Dto(
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
        StreetV7Dto(
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

    /**
     * Returns the API form of a script variant.
     */
    fun toDto(scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        BusinessPartnerScriptVariantV7Dto(
            scriptCode = scriptVariant.scriptCode,
            nameParts = scriptVariant.nameParts,
            legalEntity = LegalEntityScriptVariantV7Dto(legalName = scriptVariant.legalName, shortName = scriptVariant.shortName),
            site = SiteScriptVariantV7Dto(name = scriptVariant.siteName),
            address = AddressScriptVariantV7Dto(
                name = scriptVariant.addressName,
                physicalAddress = with(scriptVariant.physicalAddress) {
                    PhysicalAddressScriptVariantV7Dto(
                        city = city,
                        district = district,
                        street = with(street) {
                            StreetScriptVariantV7Dto(
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
                alternativeAddress = scriptVariant.alternativeAddressCity?.let { AlternativeAddressScriptVariantV7Dto(city = it) }
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
