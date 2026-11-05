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
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.AddressParseErrorMapperV6
import org.eclipse.tractusx.bpdm.pool.model.error.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AddressUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.InvalidParentBpn
import org.eclipse.tractusx.bpdm.pool.model.error.SiteMainAddressOmitted
import org.eclipse.tractusx.bpdm.pool.model.error.SiteNotInAddressLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableAddress
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableSite
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
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressGetError.AddressNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.LegalEntityAddressSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.LegalEntityAddressSearchError.LegalEntityNotFound
import org.springframework.stereotype.Component

/**
 * Maps address records and rejections to their API v6 form.
 */
@Component
class ApiV6AddressMapper(
    private val apiV6BusinessPartnerMapper: ApiV6BusinessPartnerMapper,
    private val addressParseErrorMapperV6: AddressParseErrorMapperV6
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

    private fun toCreateErrorInfo(error: AddressCreateParseError, entityKey: String?): ErrorInfoV6<AddressCreateErrorV6> =
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
            is AddressContentParseError -> addressParseErrorMapperV6.toCreateErrorInfo(error, entityKey)
        }

    private fun toUpdateErrorInfo(error: AddressUpdateParseError, entityKey: String?): ErrorInfoV6<AddressUpdateErrorV6> =
        when (error) {
            is UnresolvableAddress ->
                ErrorInfoV6(AddressUpdateErrorV6.AddressNotFound, "Address '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> addressParseErrorMapperV6.toUpdateErrorInfo(error, entityKey)
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
    private fun internalError(error: AddressUpdateParseError) =
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
}
