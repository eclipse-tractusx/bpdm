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
package org.eclipse.tractusx.bpdm.pool.mapper.apiv6

import org.eclipse.tractusx.bpdm.pool.api.v6.exception.ErrorCodeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.LegalEntityCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.LegalEntityUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.SiteCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.SiteUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressConstraintError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressFieldError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressMetadataError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressScriptVariantError
import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.AddressCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.AddressUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressIdentifierVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressStateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AlternativePostalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.PhysicalPostalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.StreetV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.error.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.RegionV6Dto
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
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
import org.eclipse.tractusx.bpdm.pool.model.output.address.AlternativePostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.GeoCoordinateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.PhysicalPostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.StreetRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.AdministrativeAreaRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressGetError.AddressNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.LegalEntityAddressSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.LegalEntityAddressSearchError.LegalEntityNotFound
import org.springframework.stereotype.Component

/**
 * Maps address records and rejections to their API v6 form.
 */
@Component
class ApiV6AddressMapper(
    private val apiV6BusinessPartnerMapper: ApiV6BusinessPartnerMapper
) {

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: AddressRecord): LogisticAddressVerboseV6Dto =
        LogisticAddressVerboseV6Dto(
            bpna = record.bpn,
            bpnLegalEntity = record.legalEntityBpn,
            bpnSite = record.siteBpn,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            name = record.name,
            states = record.states.map { toStateDto(it) },
            identifiers = record.identifiers.map { toDto(it) },
            physicalPostalAddress = toDto(record.physicalPostalAddress),
            alternativePostalAddress = record.alternativePostalAddress?.let { toDto(it) },
            confidenceCriteria = apiV6BusinessPartnerMapper.toDto(record.confidenceCriteria),
            isCatenaXMemberData = record.isDataSpaceParticipant,
            addressType = record.addressType
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
    fun toCreateResponse(requests: List<AddressPartnerCreateV6Request>, results: List<AddressCreateResult>): AddressPartnerCreateV6ResponseWrapper {
        val entities = mutableListOf<AddressPartnerCreateVerboseV6Dto>()
        val errors = mutableListOf<ErrorInfoV6<AddressCreateErrorV6>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is AddressCreateResult.Created -> entities.add(AddressPartnerCreateVerboseV6Dto(address = toDto(result.address), index = request.index))
                is AddressCreateResult.Rejected -> errors.addAll(result.errors.map { toCreateErrorInfo(it, request.index) })
            }
        }
        return AddressPartnerCreateV6ResponseWrapper(entities, errors)
    }

    /**
     * Returns the response for an update batch, each result reported under the BPN of the request at its position.
     */
    fun toUpdateResponse(requests: List<AddressPartnerUpdateV6Request>, results: List<AddressUpdateResult>): AddressPartnerUpdateV6ResponseWrapper {
        val entities = mutableListOf<LogisticAddressVerboseV6Dto>()
        val errors = mutableListOf<ErrorInfoV6<AddressUpdateErrorV6>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is AddressUpdateResult.Updated -> entities.add(toDto(result.address))
                is AddressUpdateResult.Rejected -> errors.addAll(result.errors.map { toUpdateErrorInfo(it, request.bpna) })
            }
        }
        return AddressPartnerUpdateV6ResponseWrapper(entities, errors)
    }

    private fun toCreateErrorInfo(error: AddressCreateError, entityKey: String?): ErrorInfoV6<AddressCreateErrorV6> =
        when (error) {
            is InvalidParentBpn ->
                ErrorInfoV6(AddressCreateErrorV6.BpnNotValid, "Parent '${error.bpn}' is not a valid BPNL/BPNS", entityKey)
            is UnresolvableLegalEntity ->
                ErrorInfoV6(AddressCreateErrorV6.LegalEntityNotFound, "Parent legal entity '${error.bpn}' not found", entityKey)
            is UnresolvableSite ->
                ErrorInfoV6(AddressCreateErrorV6.SiteNotFound, "Parent site '${error.bpn}' not found", entityKey)
            is SiteNotInAddressLegalEntity ->
                ErrorInfoV6(
                    AddressCreateErrorV6.SiteNotInLegalEntity,
                    "Parent site '${error.siteBpn}' does not belong to legal entity '${error.legalEntityBpn}'",
                    entityKey
                )
            is AddressContentError -> toCreateContentErrorInfo(error, entityKey)
        }

    private fun toUpdateErrorInfo(error: AddressUpdateError, entityKey: String?): ErrorInfoV6<AddressUpdateErrorV6> =
        when (error) {
            is UnresolvableAddress ->
                ErrorInfoV6(AddressUpdateErrorV6.AddressNotFound, "Address '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentError -> toUpdateContentErrorInfo(error, entityKey)
            is SiteNotInAddressLegalEntity ->
                ErrorInfoV6(
                    AddressUpdateErrorV6.SiteNotInLegalEntity,
                    "Site '${error.siteBpn}' does not belong to legal entity '${error.legalEntityBpn}'",
                    entityKey
                )
            is UnresolvableSite,
            is SiteMainAddressOmitted -> throw internalError(error)
        }

    // An error the frozen v6 enum has no code for, or that this use case cannot reach, gets no public code.
    private fun internalError(error: AddressUpdateError) =
        BpdmValidationException("Unexpected address parse error (no v6 error code): $error")

    private fun toStateDto(record: BusinessStateRecord): AddressStateVerboseV6Dto =
        AddressStateVerboseV6Dto(
            BusinessStateDates.reportValidFrom(record.validFrom),
            BusinessStateDates.reportValidTo(record.validTo),
            record.type.toDto()
        )

    private fun toDto(record: AddressIdentifierRecord): AddressIdentifierVerboseV6Dto =
        AddressIdentifierVerboseV6Dto(record.value, TypeKeyNameVerboseDto(record.typeTechnicalKey, record.typeName))

    private fun toDto(record: PhysicalPostalAddressRecord): PhysicalPostalAddressVerboseV6Dto =
        PhysicalPostalAddressVerboseV6Dto(
            geographicCoordinates = record.geographicCoordinates?.let { toDto(it) },
            countryVerbose = TypeKeyNameVerboseDto(record.country.countryCode, record.country.name),
            administrativeAreaLevel1Verbose = record.administrativeAreaLevel1?.let { toDto(it) },
            administrativeAreaLevel2 = record.administrativeAreaLevel2,
            administrativeAreaLevel3 = record.administrativeAreaLevel3,
            postalCode = record.postalCode,
            city = record.city,
            district = record.district,
            street = record.street?.let { toDto(it) },
            companyPostalCode = record.companyPostalCode,
            industrialZone = record.industrialZone,
            building = record.building,
            floor = record.floor,
            door = record.door,
            taxJurisdictionCode = record.taxJurisdictionCode
        )

    private fun toDto(record: AlternativePostalAddressRecord): AlternativePostalAddressVerboseV6Dto =
        AlternativePostalAddressVerboseV6Dto(
            geographicCoordinates = record.geographicCoordinates?.let { toDto(it) },
            countryVerbose = TypeKeyNameVerboseDto(record.country.countryCode, record.country.name),
            administrativeAreaLevel1Verbose = record.administrativeAreaLevel1?.let { toDto(it) },
            postalCode = record.postalCode,
            city = record.city,
            deliveryServiceType = record.deliveryServiceType,
            deliveryServiceQualifier = record.deliveryServiceQualifier,
            deliveryServiceNumber = record.deliveryServiceNumber
        )

    private fun toDto(record: AdministrativeAreaRecord): RegionV6Dto =
        RegionV6Dto(record.countryCode, record.code, record.name)

    private fun toDto(record: GeoCoordinateRecord): GeoCoordinateDto =
        GeoCoordinateDto(record.longitude, record.latitude, record.altitude)

    private fun toDto(record: StreetRecord): StreetV6Dto =
        StreetV6Dto(
            name = record.name,
            houseNumber = record.houseNumber,
            houseNumberSupplement = record.houseNumberSupplement,
            milestone = record.milestone,
            direction = record.direction,
            namePrefix = record.namePrefix,
            additionalNamePrefix = record.additionalNamePrefix,
            nameSuffix = record.nameSuffix,
            additionalNameSuffix = record.additionalNameSuffix
        )

    private fun toCreateContentErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<AddressCreateErrorV6> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = AddressCreateErrorV6.RegionNotFound,
            countryNotFound = AddressCreateErrorV6.CountryNotFound,
            identifierNotFound = AddressCreateErrorV6.IdentifierNotFound,
            duplicateIdentifier = AddressCreateErrorV6.AddressDuplicateIdentifier,
            identifiersTooMany = AddressCreateErrorV6.IdentifiersTooMany
        )

    private fun toUpdateContentErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<AddressUpdateErrorV6> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = AddressUpdateErrorV6.RegionNotFound,
            countryNotFound = AddressUpdateErrorV6.CountryNotFound,
            identifierNotFound = AddressUpdateErrorV6.IdentifierNotFound,
            duplicateIdentifier = AddressUpdateErrorV6.AddressDuplicateIdentifier,
            identifiersTooMany = AddressUpdateErrorV6.IdentifiersTooMany
        )

    /**
     * Returns the API error for a legal entity create rejected by its legal address content.
     */
    fun toLegalAddressCreateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<LegalEntityCreateErrorV6> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityCreateErrorV6.LegalAddressRegionNotFound,
            countryNotFound = LegalEntityCreateErrorV6.LegalAddressCountryNotFound,
            identifierNotFound = LegalEntityCreateErrorV6.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityCreateErrorV6.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityCreateErrorV6.LegalAddressIdentifiersTooMany
        )

    /**
     * Returns the API error for a legal entity update rejected by its legal address content.
     */
    fun toLegalAddressUpdateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<LegalEntityUpdateErrorV6> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityUpdateErrorV6.LegalAddressRegionNotFound,
            countryNotFound = LegalEntityUpdateErrorV6.LegalAddressCountryNotFound,
            identifierNotFound = LegalEntityUpdateErrorV6.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityUpdateErrorV6.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityUpdateErrorV6.LegalAddressIdentifiersTooMany
        )

    /**
     * Returns the API error for a site create rejected by its main address content.
     */
    fun toMainAddressCreateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<SiteCreateErrorV6> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteCreateErrorV6.MainAddressRegionNotFound,
            countryNotFound = SiteCreateErrorV6.MainAddressCountryNotFound,
            identifierNotFound = SiteCreateErrorV6.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteCreateErrorV6.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteCreateErrorV6.MainAddressIdentifiersTooMany
        )

    /**
     * Returns the API error for a site update rejected by its main address content.
     */
    fun toMainAddressUpdateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<SiteUpdateErrorV6> =
        toContentErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteUpdateErrorV6.MainAddressRegionNotFound,
            countryNotFound = SiteUpdateErrorV6.MainAddressCountryNotFound,
            identifierNotFound = SiteUpdateErrorV6.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteUpdateErrorV6.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteUpdateErrorV6.MainAddressIdentifiersTooMany
        )

    private fun <E : ErrorCodeV6> toContentErrorInfo(
        error: AddressContentError,
        entityKey: String?,
        regionNotFound: E,
        countryNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E
    ): ErrorInfoV6<E> =
        when (error) {
            is AddressFieldError -> throw internalError(error)
            is AddressMetadataError -> when (error) {
                is AddressMetadataError.IdentifierTypeNotFound ->
                    ErrorInfoV6(identifierNotFound, "Address Identifier Type '${error.type}' does not exist", entityKey)
                is AddressMetadataError.PhysicalCountryNotFound ->
                    ErrorInfoV6(countryNotFound, "Address country '${error.country}' does not exist", entityKey)
                is AddressMetadataError.AlternativeCountryNotFound ->
                    ErrorInfoV6(countryNotFound, "Address country '${error.country}' does not exist", entityKey)
                is AddressMetadataError.PhysicalRegionNotFound ->
                    ErrorInfoV6(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataError.AlternativeRegionNotFound ->
                    ErrorInfoV6(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataError.ScriptCodeNotFound -> throw internalError(error)
            }
            is AddressConstraintError -> when (error) {
                is AddressConstraintError.IdentifiersTooMany ->
                    ErrorInfoV6(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
                is AddressConstraintError.DuplicateIdentifier ->
                    ErrorInfoV6(duplicateIdentifier, "Duplicate Address Identifier: Value '${error.value}' of type '${error.type}'", entityKey)
            }
            is AddressScriptVariantError -> throw internalError(error)
        }
}
