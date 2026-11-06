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

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchAddressIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchAlternativeAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchLegalEntityV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchPhysicalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchPostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchResultV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchSiteV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.pool.exception.BusinessPartnerSearchException
import org.eclipse.tractusx.bpdm.pool.model.output.address.AlternativePostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.GeoCoordinateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.PhysicalPostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.StreetRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessPartnerSearchAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessPartnerSearchLegalEntityRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessPartnerSearchRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessPartnerSearchSiteRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.ConfidenceCriteriaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.RelationValidityPeriodRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.BusinessPartnerSearchError
import org.springframework.stereotype.Component

/**
 * Maps business partner search records and rejections, and the parts legal entity, site and address records share,
 * to their API v7 form.
 */
@Component
class ApiV7BusinessPartnerMapper {

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: ConfidenceCriteriaRecord): ConfidenceCriteriaV7Dto =
        ConfidenceCriteriaV7Dto(
            sharedByOwner = record.sharedByOwner,
            checkedByExternalDataSource = record.checkedByExternalDataSource,
            numberOfSharingMembers = record.numberOfSharingMembers,
            lastConfidenceCheckAt = record.lastConfidenceCheckAt,
            nextConfidenceCheckAt = record.nextConfidenceCheckAt,
            confidenceLevel = record.confidenceLevel
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: RelationValidityPeriodRecord): RelationValidityPeriodV7Dto =
        RelationValidityPeriodV7Dto(validFrom = record.validFrom, validTo = record.validTo)

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: GeoCoordinateRecord): GeoCoordinateDto =
        GeoCoordinateDto(longitude = record.longitude, latitude = record.latitude, altitude = record.altitude)

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: StreetRecord): StreetV7Dto =
        StreetV7Dto(
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

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: BusinessPartnerSearchRecord): BusinessPartnerSearchResultV7Dto =
        BusinessPartnerSearchResultV7Dto(
            identifiers = record.legalEntity.identifiers.map {
                BusinessPartnerSearchIdentifierV7Dto(type = it.typeTechnicalKey, value = it.value, issuingBody = it.issuingBody)
            },
            states = record.legalEntity.states.map { toSearchStateDto(it) },
            legalEntity = toSearchDto(record.legalEntity),
            site = record.site?.let { toSearchDto(it) },
            address = toSearchDto(record.address),
            isParticipantData = record.legalEntity.isDataSpaceParticipant
        )

    /**
     * Returns the exception reporting every one of [errors] in a single message.
     */
    fun toSearchException(errors: List<BusinessPartnerSearchError>): BusinessPartnerSearchException =
        BusinessPartnerSearchException(errors.joinToString(" ") { toSearchErrorMessage(it) })

    private fun toSearchDto(record: BusinessPartnerSearchLegalEntityRecord): BusinessPartnerSearchLegalEntityV7Dto =
        BusinessPartnerSearchLegalEntityV7Dto(
            legalEntityBpn = record.bpn,
            legalName = record.legalName,
            shortName = record.legalShortName,
            legalForm = record.legalFormTechnicalKey,
            confidenceCriteria = toSearchConfidenceCriteriaDto(record.confidenceCriteria),
            states = record.states.map { toSearchStateDto(it) }
        )

    private fun toSearchDto(record: BusinessPartnerSearchSiteRecord): BusinessPartnerSearchSiteV7Dto =
        BusinessPartnerSearchSiteV7Dto(
            siteBpn = record.bpn,
            name = record.name,
            confidenceCriteria = toSearchConfidenceCriteriaDto(record.confidenceCriteria),
            states = record.states.map { toSearchStateDto(it) }
        )

    private fun toSearchDto(record: BusinessPartnerSearchAddressRecord): BusinessPartnerSearchPostalAddressV7Dto =
        BusinessPartnerSearchPostalAddressV7Dto(
            name = record.name,
            addressType = record.addressType,
            identifiers = record.identifiers.map { BusinessPartnerSearchAddressIdentifierV7Dto(type = it.typeTechnicalKey, value = it.value) },
            addressBpn = record.bpn,
            states = record.states.map { toSearchStateDto(it) },
            physicalPostalAddress = toSearchDto(record.physicalPostalAddress),
            alternativePostalAddress = toSearchDto(record.alternativePostalAddress),
            confidenceCriteria = toSearchConfidenceCriteriaDto(record.confidenceCriteria)
        )

    private fun toSearchDto(record: PhysicalPostalAddressRecord): BusinessPartnerSearchPhysicalAddressV7Dto =
        BusinessPartnerSearchPhysicalAddressV7Dto(
            geographicCoordinates = record.geographicCoordinates?.let { toDto(it) },
            administrativeAreaLevel1 = record.administrativeAreaLevel1?.code,
            administrativeAreaLevel2 = record.administrativeAreaLevel2,
            administrativeAreaLevel3 = record.administrativeAreaLevel3,
            street = record.street?.let { toDto(it) },
            postalCode = record.postalCode,
            city = record.city,
            country = record.country.countryCode,
            district = record.district,
            companyPostalCode = record.companyPostalCode,
            industrialZone = record.industrialZone,
            building = record.building,
            floor = record.floor,
            door = record.door,
            taxJurisdictionCode = record.taxJurisdictionCode
        )

    // The search API reports a missing alternative address as one with every field empty rather than as null.
    private fun toSearchDto(record: AlternativePostalAddressRecord?): BusinessPartnerSearchAlternativeAddressV7Dto =
        BusinessPartnerSearchAlternativeAddressV7Dto(
            geographicCoordinates = record?.geographicCoordinates?.let { toDto(it) },
            country = record?.country?.countryCode,
            administrativeAreaLevel1 = record?.administrativeAreaLevel1?.code,
            postalCode = record?.postalCode,
            city = record?.city,
            deliveryServiceType = record?.deliveryServiceType,
            deliveryServiceQualifier = record?.deliveryServiceQualifier,
            deliveryServiceNumber = record?.deliveryServiceNumber
        )

    private fun toSearchErrorMessage(error: BusinessPartnerSearchError): String =
        when (error) {
            BusinessPartnerSearchError.BpnAndLegalNameMissing -> "At least one of 'bpn' or 'legalName' must be provided."
            BusinessPartnerSearchError.NoBusinessPartnerKindIncluded -> "At least one filter value must be provided in 'searchResultFilter'."
            BusinessPartnerSearchError.LeadingWhitespace -> "'bpn' and 'legalName' must not start with a whitespace character."
            is BusinessPartnerSearchError.LegalNameTooShort -> "'legalName' must contain at least ${error.minLength} characters."
        }

    private fun toSearchConfidenceCriteriaDto(record: ConfidenceCriteriaRecord): BusinessPartnerSearchConfidenceCriteriaV7Dto =
        BusinessPartnerSearchConfidenceCriteriaV7Dto(
            sharedByOwner = record.sharedByOwner,
            checkedByExternalDataSource = record.checkedByExternalDataSource,
            numberOfSharingMembers = record.numberOfSharingMembers,
            lastConfidenceCheckAt = record.lastConfidenceCheckAt,
            nextConfidenceCheckAt = record.nextConfidenceCheckAt,
            confidenceLevel = record.confidenceLevel
        )

    private fun toSearchStateDto(record: BusinessStateRecord): BusinessPartnerSearchStateV7Dto =
        BusinessPartnerSearchStateV7Dto(
            validFrom = BusinessStateDates.reportValidFrom(record.validFrom),
            validTo = BusinessStateDates.reportValidTo(record.validTo),
            type = record.type
        )
}
