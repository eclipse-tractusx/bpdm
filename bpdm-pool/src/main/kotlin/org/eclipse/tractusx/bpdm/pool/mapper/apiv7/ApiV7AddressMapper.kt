/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package org.eclipse.tractusx.bpdm.pool.mapper.apiv7

import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.ErrorCodeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.LegalEntityCreateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.LegalEntityUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.SiteCreateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.SiteUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressConstraintError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressFieldError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressMetadataError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressScriptVariantError
import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.AddressCreateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.AddressUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.error.ErrorInfoV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.RegionV7Dto
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressContentError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressCreateError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressUpdateError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.InvalidParentBpn
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.SiteMainAddressOmitted
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.SiteNotInAddressLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.UnresolvableAddress
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.UnresolvableLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.UnresolvableSite
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressCreateResult
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressUpdateResult
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressIdentifierRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AlternativePostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.PhysicalAddressScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.PhysicalPostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.StreetScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.AdministrativeAreaRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressGetError.AddressNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.LegalEntityAddressSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.LegalEntityAddressSearchError.LegalEntityNotFound
import org.springframework.stereotype.Component

/**
 * Maps address records and rejections to their API v7 form.
 */
@Component
class ApiV7AddressMapper(
    private val apiV7BusinessPartnerMapper: ApiV7BusinessPartnerMapper
) {

    /**
     * Returns the API DTO for [record], together with its script variants.
     */
    fun toDto(record: AddressRecord): LogisticAddressVerboseV7Dto =
        LogisticAddressVerboseV7Dto(
            address = toInvariantDto(record),
            scriptVariants = toScriptVariantDtos(record)
        )

    /**
     * Returns the exception the API reports a rejected address fetch with.
     */
    fun toGetException(errors: List<AddressGetError>): BpdmNotFoundException =
        when (val error = errors.first()) {
            is AddressNotFound -> BpdmNotFoundException("Address", error.bpn)
        }

    /**
     * Returns the exception the API reports a rejected search for the addresses of a legal entity with.
     */
    fun toLegalEntityAddressSearchException(errors: List<LegalEntityAddressSearchError>): BpdmNotFoundException =
        when (val error = errors.first()) {
            is LegalEntityNotFound -> BpdmNotFoundException("Business Partner", error.bpn)
        }

    /**
     * Returns the response for a create batch, each result reported under the index of the request at its position.
     */
    fun toCreateResponse(requests: List<AddressPartnerCreateV7Request>, results: List<AddressCreateResult>): AddressPartnerCreateV7ResponseWrapper {
        val entities = mutableListOf<AddressPartnerCreateVerboseV7Dto>()
        val errors = mutableListOf<ErrorInfoV7<AddressCreateErrorV7>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is AddressCreateResult.Created -> entities.add(toCreateDto(result.address, request.index))
                is AddressCreateResult.Rejected -> errors.addAll(result.errors.map { toCreateErrorInfo(it, request.index) })
            }
        }
        return AddressPartnerCreateV7ResponseWrapper(entities, errors)
    }

    /**
     * Returns the response for an update batch, each result reported under the BPN of the request at its position.
     */
    fun toUpdateResponse(requests: List<AddressPartnerUpdateV7Request>, results: List<AddressUpdateResult>): AddressPartnerUpdateV7ResponseWrapper {
        val entities = mutableListOf<AddressPartnerUpdateVerboseV7Dto>()
        val errors = mutableListOf<ErrorInfoV7<AddressUpdateErrorV7>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is AddressUpdateResult.Updated -> entities.add(toUpdateDto(result.address))
                is AddressUpdateResult.Rejected -> errors.addAll(result.errors.map { toUpdateErrorInfo(it, request.bpna) })
            }
        }
        return AddressPartnerUpdateV7ResponseWrapper(entities, errors)
    }

    /**
     * Returns the script-invariant part of [record] as the API reports it.
     */
    fun toInvariantDto(record: AddressRecord): LogisticAddressInvariantVerboseV7Dto =
        LogisticAddressInvariantVerboseV7Dto(
            bpna = record.bpn,
            bpnLegalEntity = record.legalEntityBpn,
            bpnSite = record.siteBpn,
            additionalSites = record.additionalSiteBpns,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            name = record.name,
            states = record.states.map { toStateDto(it) },
            identifiers = record.identifiers.map { toDto(it) },
            relations = record.relations.map { toDto(it) },
            physicalPostalAddress = toDto(record.physicalPostalAddress),
            alternativePostalAddress = record.alternativePostalAddress?.let { toDto(it) },
            confidenceCriteria = apiV7BusinessPartnerMapper.toDto(record.confidenceCriteria),
            isParticipantData = record.isDataSpaceParticipant,
            addressType = record.addressType
        )

    /**
     * Returns the postal address [record] holds in its script as the API reports it.
     */
    fun toPostalAddressDto(record: AddressScriptVariantRecord): PostalAddressScriptVariantV7Dto =
        PostalAddressScriptVariantV7Dto(
            addressName = record.name,
            physicalAddress = toDto(record.physicalAddress),
            alternativeAddress = record.alternativeAddress?.let { AlternativeAddressScriptVariantV7Dto(it.city) }
        )

    private fun toCreateDto(record: AddressRecord, entityKey: String?): AddressPartnerCreateVerboseV7Dto =
        AddressPartnerCreateVerboseV7Dto(
            address = toInvariantDto(record),
            scriptVariants = toScriptVariantDtos(record),
            index = entityKey
        )

    private fun toUpdateDto(record: AddressRecord): AddressPartnerUpdateVerboseV7Dto =
        AddressPartnerUpdateVerboseV7Dto(
            address = toInvariantDto(record),
            scriptVariants = toScriptVariantDtos(record)
        )

    private fun toScriptVariantDtos(record: AddressRecord): List<LogisticAddressScriptVariantV7Dto> =
        record.scriptVariants.map { LogisticAddressScriptVariantV7Dto(it.scriptCode, toPostalAddressDto(it)) }

    private fun toCreateErrorInfo(error: AddressCreateError, entityKey: String?): ErrorInfoV7<AddressCreateErrorV7> =
        when (error) {
            is InvalidParentBpn ->
                ErrorInfoV7(AddressCreateErrorV7.BpnNotValid, "Parent '${error.bpn}' is not a valid BPNL/BPNS", entityKey)
            is UnresolvableLegalEntity ->
                ErrorInfoV7(AddressCreateErrorV7.LegalEntityNotFound, "Parent legal entity '${error.bpn}' not found", entityKey)
            is UnresolvableSite ->
                ErrorInfoV7(AddressCreateErrorV7.SiteNotFound, "Parent site '${error.bpn}' not found", entityKey)
            is SiteNotInAddressLegalEntity ->
                ErrorInfoV7(
                    AddressCreateErrorV7.SiteNotInLegalEntity,
                    "Parent site '${error.siteBpn}' does not belong to legal entity '${error.legalEntityBpn}'",
                    entityKey
                )
            is AddressContentError -> toCreateContentErrorInfo(error, entityKey)
        }

    private fun toUpdateErrorInfo(error: AddressUpdateError, entityKey: String?): ErrorInfoV7<AddressUpdateErrorV7> =
        when (error) {
            is UnresolvableAddress ->
                ErrorInfoV7(AddressUpdateErrorV7.AddressNotFound, "Address '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentError -> toUpdateContentErrorInfo(error, entityKey)
            is SiteNotInAddressLegalEntity ->
                ErrorInfoV7(
                    AddressUpdateErrorV7.SiteNotInLegalEntity,
                    "Site '${error.siteBpn}' does not belong to legal entity '${error.legalEntityBpn}'",
                    entityKey
                )
            is UnresolvableSite ->
                ErrorInfoV7(AddressUpdateErrorV7.SiteNotFound, "Site '${error.bpn}' not found", entityKey)
            is SiteMainAddressOmitted ->
                ErrorInfoV7(
                    AddressUpdateErrorV7.SiteMainAddressOmitted,
                    "Site '${error.siteBpn}' has this address as its main address and must stay among its sites",
                    entityKey
                )
        }

    private fun toStateDto(record: BusinessStateRecord): AddressStateVerboseV7Dto =
        AddressStateVerboseV7Dto(
            BusinessStateDates.reportValidFrom(record.validFrom),
            BusinessStateDates.reportValidTo(record.validTo),
            record.type.toDto()
        )

    private fun toDto(record: AddressIdentifierRecord): AddressIdentifierVerboseV7Dto =
        AddressIdentifierVerboseV7Dto(record.value, TypeKeyNameVerboseDto(record.typeTechnicalKey, record.typeName))

    private fun toDto(record: AddressRelationRecord): AddressRelationVerboseV7Dto =
        AddressRelationVerboseV7Dto(
            type = toDto(record.type),
            businessPartnerSourceBpna = record.sourceBpna,
            businessPartnerTargetBpna = record.targetBpna,
            validityPeriods = record.validityPeriods.map { apiV7BusinessPartnerMapper.toDto(it) },
            reasonCode = record.reasonCode
        )

    private fun toDto(record: PhysicalPostalAddressRecord): PhysicalPostalAddressVerboseV7Dto =
        PhysicalPostalAddressVerboseV7Dto(
            geographicCoordinates = record.geographicCoordinates?.let { apiV7BusinessPartnerMapper.toDto(it) },
            countryVerbose = TypeKeyNameVerboseDto(record.country.countryCode, record.country.name),
            postalCode = record.postalCode,
            city = record.city,
            administrativeAreaLevel1Verbose = record.administrativeAreaLevel1?.let { toDto(it) },
            administrativeAreaLevel2 = record.administrativeAreaLevel2,
            administrativeAreaLevel3 = record.administrativeAreaLevel3,
            district = record.district,
            companyPostalCode = record.companyPostalCode,
            industrialZone = record.industrialZone,
            building = record.building,
            floor = record.floor,
            door = record.door,
            street = record.street?.let { apiV7BusinessPartnerMapper.toDto(it) },
            taxJurisdictionCode = record.taxJurisdictionCode
        )

    private fun toDto(record: AlternativePostalAddressRecord): AlternativePostalAddressVerboseV7Dto =
        AlternativePostalAddressVerboseV7Dto(
            geographicCoordinates = record.geographicCoordinates?.let { apiV7BusinessPartnerMapper.toDto(it) },
            countryVerbose = TypeKeyNameVerboseDto(record.country.countryCode, record.country.name),
            postalCode = record.postalCode,
            city = record.city,
            administrativeAreaLevel1Verbose = record.administrativeAreaLevel1?.let { toDto(it) },
            deliveryServiceType = record.deliveryServiceType,
            deliveryServiceNumber = record.deliveryServiceNumber,
            deliveryServiceQualifier = record.deliveryServiceQualifier
        )

    private fun toDto(record: AdministrativeAreaRecord): RegionV7Dto =
        RegionV7Dto(record.countryCode, record.code, record.name)

    private fun toDto(record: PhysicalAddressScriptVariantRecord): PhysicalAddressScriptVariantV7Dto =
        PhysicalAddressScriptVariantV7Dto(
            city = record.city,
            district = record.district,
            street = record.street?.let { toDto(it) },
            industrialZone = record.industrialZone,
            building = record.building,
            floor = record.floor,
            door = record.door
        )

    private fun toDto(record: StreetScriptVariantRecord): StreetScriptVariantV7Dto =
        StreetScriptVariantV7Dto(
            name = record.name,
            direction = record.direction,
            namePrefix = record.namePrefix,
            additionalNamePrefix = record.additionalNamePrefix,
            nameSuffix = record.nameSuffix,
            additionalNameSuffix = record.additionalNameSuffix
        )

    private fun toCreateContentErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV7<AddressCreateErrorV7> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = AddressCreateErrorV7.RegionNotFound,
            countryNotFound = AddressCreateErrorV7.CountryNotFound,
            identifierNotFound = AddressCreateErrorV7.IdentifierNotFound,
            duplicateIdentifier = AddressCreateErrorV7.AddressDuplicateIdentifier,
            identifiersTooMany = AddressCreateErrorV7.IdentifiersTooMany,
            scriptVariantCityMissing = AddressCreateErrorV7.ScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = AddressCreateErrorV7.ScriptVariantDuplicateScriptCode
        )

    private fun toUpdateContentErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV7<AddressUpdateErrorV7> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = AddressUpdateErrorV7.RegionNotFound,
            countryNotFound = AddressUpdateErrorV7.CountryNotFound,
            identifierNotFound = AddressUpdateErrorV7.IdentifierNotFound,
            duplicateIdentifier = AddressUpdateErrorV7.AddressDuplicateIdentifier,
            identifiersTooMany = AddressUpdateErrorV7.IdentifiersTooMany,
            scriptVariantCityMissing = AddressUpdateErrorV7.ScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = AddressUpdateErrorV7.ScriptVariantDuplicateScriptCode
        )

    /**
     * Returns the API error for a legal entity create rejected by its legal address content.
     */
    fun toLegalAddressCreateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV7<LegalEntityCreateErrorV7> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityCreateErrorV7.LegalAddressRegionNotFound,
            countryNotFound = LegalEntityCreateErrorV7.LegalAddressCountryNotFound,
            identifierNotFound = LegalEntityCreateErrorV7.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityCreateErrorV7.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityCreateErrorV7.LegalAddressIdentifiersTooMany,
            scriptVariantCityMissing = LegalEntityCreateErrorV7.LegalAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = LegalEntityCreateErrorV7.LegalAddressScriptVariantDuplicateScriptCode
        )

    /**
     * Returns the API error for a legal entity update rejected by its legal address content.
     */
    fun toLegalAddressUpdateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV7<LegalEntityUpdateErrorV7> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityUpdateErrorV7.LegalAddressRegionNotFound,
            countryNotFound = LegalEntityUpdateErrorV7.LegalAddressCountryNotFound,
            identifierNotFound = LegalEntityUpdateErrorV7.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityUpdateErrorV7.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityUpdateErrorV7.LegalAddressIdentifiersTooMany,
            scriptVariantCityMissing = LegalEntityUpdateErrorV7.LegalAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = LegalEntityUpdateErrorV7.LegalAddressScriptVariantDuplicateScriptCode
        )

    /**
     * Returns the API error for a site create rejected by its main address content.
     */
    fun toMainAddressCreateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV7<SiteCreateErrorV7> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteCreateErrorV7.MainAddressRegionNotFound,
            countryNotFound = SiteCreateErrorV7.MainAddressCountryNotFound,
            identifierNotFound = SiteCreateErrorV7.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteCreateErrorV7.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteCreateErrorV7.MainAddressIdentifiersTooMany,
            scriptVariantCityMissing = SiteCreateErrorV7.MainAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = SiteCreateErrorV7.MainAddressScriptVariantDuplicateScriptCode
        )

    /**
     * Returns the API error for a site update rejected by its main address content.
     */
    fun toMainAddressUpdateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV7<SiteUpdateErrorV7> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteUpdateErrorV7.MainAddressRegionNotFound,
            countryNotFound = SiteUpdateErrorV7.MainAddressCountryNotFound,
            identifierNotFound = SiteUpdateErrorV7.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteUpdateErrorV7.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteUpdateErrorV7.MainAddressIdentifiersTooMany,
            scriptVariantCityMissing = SiteUpdateErrorV7.MainAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = SiteUpdateErrorV7.MainAddressScriptVariantDuplicateScriptCode
        )

    private fun <E : ErrorCodeV7> toContentErrorInfo(
        error: AddressContentError,
        entityKey: String?,
        regionNotFound: E,
        countryNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E,
        scriptVariantCityMissing: E,
        scriptVariantDuplicateScriptCode: E
    ): ErrorInfoV7<E> =
        when (error) {
            is AddressFieldError -> throw internalError(error)
            is AddressMetadataError -> when (error) {
                is AddressMetadataError.IdentifierTypeNotFound ->
                    ErrorInfoV7(identifierNotFound, "Address Identifier Type '${error.type}' does not exist", entityKey)
                is AddressMetadataError.PhysicalCountryNotFound ->
                    ErrorInfoV7(countryNotFound, "Address country '${error.country}' does not exist", entityKey)
                is AddressMetadataError.AlternativeCountryNotFound ->
                    ErrorInfoV7(countryNotFound, "Address country '${error.country}' does not exist", entityKey)
                is AddressMetadataError.PhysicalRegionNotFound ->
                    ErrorInfoV7(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataError.AlternativeRegionNotFound ->
                    ErrorInfoV7(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataError.ScriptCodeNotFound -> throw internalError(error)
            }
            is AddressConstraintError -> when (error) {
                is AddressConstraintError.IdentifiersTooMany ->
                    ErrorInfoV7(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
                is AddressConstraintError.DuplicateIdentifier ->
                    ErrorInfoV7(duplicateIdentifier, "Duplicate Address Identifier: Value '${error.value}' of type '${error.type}'", entityKey)
            }
            is AddressScriptVariantError -> when (error) {
                is AddressScriptVariantError.PhysicalCityMissing ->
                    ErrorInfoV7(scriptVariantCityMissing, "Script variant ${error.index} has no city in its physical address", entityKey)
                is AddressScriptVariantError.AlternativeCityMissing ->
                    ErrorInfoV7(scriptVariantCityMissing, "Script variant ${error.index} has no city in its alternative address", entityKey)
                is AddressScriptVariantError.DuplicateScriptCode ->
                    ErrorInfoV7(scriptVariantDuplicateScriptCode, "Duplicate address script variant for script code '${error.scriptCode}'", entityKey)
            }
        }

    private fun internalError(error: Any) =
        BpdmValidationException("Unexpected address validation error that has no client error code: $error")

    private fun toDto(relationType: AddressRelationType): AddressRelationTypeV7 =
        when (relationType) {
            AddressRelationType.IsReplacedBy -> AddressRelationTypeV7.IsReplacedBy
        }
}
