/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinate
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerbose
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageDto<T> {
    return PageDto(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

fun LegalEntityDb.toMatchDto(score: Float): LegalEntityMatchVerboseResponse {
    return LegalEntityMatchVerboseResponse(
        score = score,
        legalEntity = this.toDto(),
        legalAddress = legalAddress.toDto(),
    )
}

fun LegalEntityDb.toUpsertDto(entryId: String?): LegalEntityPartnerCreateVerboseResponse {
    return LegalEntityPartnerCreateVerboseResponse(
        legalEntity = toDto(),
        legalAddress = legalAddress.toDto(),
        index = entryId
    )
}

fun LegalEntityDb.toLegalEntityWithLegalAddress(): LegalEntityWithLegalAddressVerboseResponse {
    return LegalEntityWithLegalAddressVerboseResponse(
        legalAddress = legalAddress.toDto(),
        legalEntity = toDto()
    )
}

fun LegalEntityDb.toDto(): LegalEntityVerbose {
    return LegalEntityVerbose(
        bpnl = bpn,
        legalName = legalName.value,
        legalShortName = legalName.shortName,
        legalFormVerbose = legalForm?.toDto(),
        identifiers = identifiers.map { it.toDto() },
        states = states.map { it.toDto() },
        classifications = classifications.map { it.toDto() },
        relations = startNodeRelations.plus(endNodeRelations).map { it.toDto() },
        currentness = currentness,
        confidenceCriteria = confidenceCriteria.toDto(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun LegalEntityIdentifierDb.toDto(): LegalEntityIdentifierVerbose {
    return LegalEntityIdentifierVerbose(value, type.toTypeKeyNameDto(), issuingBody)
}

fun AddressIdentifierDb.toDto(): AddressIdentifierVerbose {
    return AddressIdentifierVerbose(value, type.toTypeKeyNameDto())
}

fun IdentifierTypeDb.toTypeKeyNameDto(): TypeKeyNameVerbose<String> {
    return TypeKeyNameVerbose(technicalKey, name)
}

fun IdentifierTypeDb.toDto(): IdentifierType {
    return IdentifierType(technicalKey, businessPartnerType, name,
        details.map { IdentifierTypeDetail(it.countryCode, it.mandatory) })
}

fun LegalFormDb.toDto(): LegalForm {
    return LegalForm(technicalKey, name, abbreviation)
}

fun LegalEntityStateDb.toDto(): LegalEntityStateVerbose {
    return LegalEntityStateVerbose(validFrom, validTo, type.toDto())
}

fun SiteStateDb.toDto(): SiteStateVerbose {
    return SiteStateVerbose(validFrom, validTo, type.toDto())
}

fun AddressStateDb.toDto(): AddressStateVerbose {
    return AddressStateVerbose(validFrom, validTo, type.toDto())
}

fun LogisticAddressDb.toDto(): LogisticAddressVerbose {
    return LogisticAddressVerbose(
        bpna = bpn,
        bpnLegalEntity = legalEntity?.bpn,
        isLegalAddress = legalEntity?.legalAddress == this,
        bpnSite = site?.bpn,
        isMainAddress = site?.mainAddress == this,
        createdAt = createdAt,
        updatedAt = updatedAt,
        name = name,
        states = states.map { it.toDto() },
        identifiers = identifiers.map { it.toDto() },
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        confidenceCriteria = confidenceCriteria.toDto(),
        addressType = getAddressType(this)
    )
}

fun LogisticAddressDb.toLegalAddressResponse(): LegalAddressVerboseResponse {
    return LegalAddressVerboseResponse(
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        bpnLegalEntity = legalEntity?.bpn!!,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun LogisticAddressDb.toMainAddressResponse(): MainAddressVerboseResponse {
    return MainAddressVerboseResponse(
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        bpnSite = site?.bpn!!,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun PhysicalPostalAddressDb.toDto(): PhysicalPostalAddressVerbose {
    return PhysicalPostalAddressVerbose(
        geographicCoordinates = geographicCoordinates?.toDto(),
        countryVerbose = country.toDto(),
        postalCode = postCode,
        city = city,
        administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { Region(it.countryCode, it.regionCode, it.regionName) },
        administrativeAreaLevel2 = administrativeAreaLevel2,
        administrativeAreaLevel3 = administrativeAreaLevel3,
        district = districtLevel1,
        companyPostalCode = companyPostCode,
        industrialZone = industrialZone,
        building = building,
        floor = floor,
        door = door,
        street = street?.toDto()
    )
}

fun AlternativePostalAddressDb.toDto(): AlternativePostalAddressVerboseDto {
    return AlternativePostalAddressVerboseDto(
        geographicCoordinates = geographicCoordinates?.toDto(),
        countryVerbose = country.toDto(),
        postalCode = postCode,
        city = city,
        administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { Region(it.countryCode, it.regionCode, it.regionName) },
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber,
        deliveryServiceQualifier = deliveryServiceQualifier
    )
}

private fun StreetDb.toDto(): Street {
    return Street(
        name = name,
        houseNumber = houseNumber,
        houseNumberSupplement = houseNumberSupplement,
        milestone = milestone,
        direction = direction,
        namePrefix = namePrefix,
        additionalNamePrefix = additionalNamePrefix,
        nameSuffix = nameSuffix,
        additionalNameSuffix = additionalNameSuffix
    )
}

fun LogisticAddressDb.toMatchDto(score: Float): AddressMatchVerboseResponse {
    return AddressMatchVerboseResponse(score, this.toDto())
}

fun LogisticAddressDb.toCreateResponse(index: String?): AddressPartnerCreateVerboseResponse {
    return AddressPartnerCreateVerboseResponse(
        address = toDto(),
        index = index
    )
}

fun SiteDb.toMatchDto(): SiteMatchVerboseResponse {
    return SiteMatchVerboseResponse(
        mainAddress = this.mainAddress.toDto(),
        site = this.toDto(),
    )
}

fun SiteDb.toUpsertDto(entryId: String?): SitePartnerCreateVerboseResponse {
    return SitePartnerCreateVerboseResponse(
        site = toDto(),
        mainAddress = mainAddress.toDto(),
        index = entryId
    )
}

fun SiteDb.toDto(): SiteVerbose {
    return SiteVerbose(
        bpn,
        name,
        states = states.map { it.toDto() },
        bpnLegalEntity = legalEntity.bpn,
        confidenceCriteria = confidenceCriteria.toDto(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun SiteDb.toPoolDto(): SiteWithMainAddressVerboseResponse {
    return SiteWithMainAddressVerboseResponse(

        site = SiteVerbose(
            bpn,
            name,
            states = states.map { it.toDto() },
            bpnLegalEntity = legalEntity.bpn,
            confidenceCriteria = confidenceCriteria.toDto(),
            createdAt = createdAt,
            updatedAt = updatedAt,
        ),
        mainAddress = mainAddress.toDto()
    )
}


fun GeographicCoordinateDb.toDto(): GeoCoordinate {
    return GeoCoordinate(longitude, latitude, altitude)
}

fun LegalEntityClassificationDb.toDto(): LegalEntityClassificationVerbose {
    return LegalEntityClassificationVerbose(value, code, type.toDto())
}

fun RelationDb.toDto(): RelationVerbose {
    return RelationVerbose(
        type = type.toDto(),
        startBpnl = startNode.bpn,
        endBpnl = endNode.bpn,
        validFrom = validFrom,
        validTo = validTo
    )
}

fun PartnerChangelogEntryDb.toDto(): ChangelogEntryVerboseResponse {
    return ChangelogEntryVerboseResponse(bpn, businessPartnerType, updatedAt, changelogType)
}

fun RegionDb.toRegionDto(): Region {
    return Region(countryCode = countryCode, regionCode = regionCode, regionName = regionName)
}

fun RegionDb.toCountrySubdivisionDto(): CountrySubdivision {
    return CountrySubdivision(countryCode = countryCode, code = regionCode, name = regionName)
}

fun ConfidenceCriteriaDb.toDto(): ConfidenceCriteria =
    ConfidenceCriteria(
        sharedByOwner,
        checkedByExternalDataSource,
        numberOfBusinessPartners,
        lastConfidenceCheckAt,
        nextConfidenceCheckAt,
        confidenceLevel
    )

private fun getAddressType(logisticAddress: LogisticAddressDb): AddressType {
    return when {
        logisticAddress.legalEntity?.legalAddress == logisticAddress &&
                logisticAddress.site?.mainAddress == logisticAddress -> AddressType.LegalAndSiteMainAddress

        logisticAddress.legalEntity?.legalAddress != logisticAddress &&
                logisticAddress.site?.mainAddress != logisticAddress -> AddressType.AdditionalAddress

        logisticAddress.legalEntity?.legalAddress == logisticAddress -> AddressType.LegalAddress

        logisticAddress.site?.mainAddress == logisticAddress -> AddressType.SiteMainAddress

        else -> throw IllegalStateException("Unable to determine address type.")
    }
}