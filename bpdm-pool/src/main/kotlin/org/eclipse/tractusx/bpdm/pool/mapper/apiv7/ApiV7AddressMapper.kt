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
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierVerboseV7Dto
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
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.RegionV7Dto
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressIdentifierRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AlternativePostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.GeoCoordinateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.PhysicalAddressScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.PhysicalPostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.StreetRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.StreetScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.AdministrativeAreaRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressGetError.AddressNotFound
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
            scriptVariants = record.scriptVariants.map { LogisticAddressScriptVariantV7Dto(it.scriptCode, toPostalAddressDto(it)) }
        )

    /**
     * Returns the exception the API reports a rejected address fetch with.
     */
    fun toGetException(errors: List<AddressGetError>): BpdmNotFoundException =
        when (val error = errors.first()) {
            is AddressNotFound -> BpdmNotFoundException("Address", error.bpn)
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
            type = record.type,
            businessPartnerSourceBpna = record.sourceBpna,
            businessPartnerTargetBpna = record.targetBpna,
            validityPeriods = record.validityPeriods.map { apiV7BusinessPartnerMapper.toDto(it) },
            reasonCode = record.reasonCode
        )

    private fun toDto(record: PhysicalPostalAddressRecord): PhysicalPostalAddressVerboseV7Dto =
        PhysicalPostalAddressVerboseV7Dto(
            geographicCoordinates = record.geographicCoordinates?.let { toDto(it) },
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
            street = record.street?.let { toDto(it) },
            taxJurisdictionCode = record.taxJurisdictionCode
        )

    private fun toDto(record: AlternativePostalAddressRecord): AlternativePostalAddressVerboseV7Dto =
        AlternativePostalAddressVerboseV7Dto(
            geographicCoordinates = record.geographicCoordinates?.let { toDto(it) },
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

    private fun toDto(record: GeoCoordinateRecord): GeoCoordinateDto =
        GeoCoordinateDto(record.longitude, record.latitude, record.altitude)

    private fun toDto(record: StreetRecord): StreetV7Dto =
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
}
