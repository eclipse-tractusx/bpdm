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
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageDto<T> {
    return PageDto(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

fun <S, T> Page<S>.toDto(map: (S) -> T): PageDto<T> {
    return PageDto(totalElements, totalPages, number, numberOfElements, content.map { map(it) })
}


fun LegalEntityDb.toMatchDto(score: Float): LegalEntityMatchVerboseDto {
    return LegalEntityMatchVerboseDto(
        score = score,
        legalEntity = this.toDto(),
        legalAddress = legalAddress.toDto(),
    )
}

fun LegalEntityDb.toUpsertDto(entryId: String?): LegalEntityPartnerCreateVerboseDto {
    return LegalEntityPartnerCreateVerboseDto(
        legalEntity = toDto(),
        legalAddress = legalAddress.toDto(),
        index = entryId
    )
}

fun LegalEntityDb.toLegalEntityWithLegalAddress(): LegalEntityWithLegalAddressVerboseDto {
    return LegalEntityWithLegalAddressVerboseDto(
        legalAddress = legalAddress.toDto(),
        legalEntity = toDto()
    )
}

fun LegalEntityDb.toDto(): LegalEntityVerboseDto {
    return LegalEntityVerboseDto(
        bpnl = bpn,
        legalName = legalName.value,
        legalShortName = legalName.shortName,
        legalFormVerbose = legalForm?.toDto(),
        identifiers = identifiers.map { it.toDto() },
        states = states.map { it.toDto() },
        relations = startNodeRelations.plus(endNodeRelations).map { it.toDto() },
        currentness = currentness,
        confidenceCriteria = confidenceCriteria.toDto(),
        isParticipantData = isCatenaXMemberData,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun LegalEntityIdentifierDb.toDto(): LegalEntityIdentifierVerboseDto {
    return LegalEntityIdentifierVerboseDto(value, type.toTypeKeyNameDto(), issuingBody)
}

fun AddressIdentifierDb.toDto(): AddressIdentifierVerboseDto {
    return AddressIdentifierVerboseDto(value, type.toTypeKeyNameDto())
}

fun IdentifierTypeDb.toTypeKeyNameDto(): TypeKeyNameVerboseDto<String> {
    return TypeKeyNameVerboseDto(technicalKey, name)
}

fun IdentifierTypeDb.toDto(): IdentifierTypeDto {
    return IdentifierTypeDto(
        technicalKey = technicalKey,
        businessPartnerType = businessPartnerType,
        name = name,
        abbreviation = abbreviation,
        transliteratedName = transliteratedName,
        transliteratedAbbreviation = transliteratedAbbreviation,
        format = format,
        categories = categories.ifEmpty { mutableSetOf(IdentifierTypeCategory.OTH) }.toSortedSet(),
        details = details.map { IdentifierTypeDetailDto(it.countryCode, it.mandatory) })
}

fun LegalFormDb.toDto(): LegalFormDto {
    return LegalFormDto(
        technicalKey = technicalKey,
        name = name,
        transliteratedName = transliteratedName,
        abbreviations = abbreviation,
        transliteratedAbbreviations = transliteratedAbbreviations,
        country = countryCode,
        language = languageCode,
        administrativeAreaLevel1 = administrativeArea?.regionCode,
        isActive = isActive
    )
}

fun LegalEntityStateDb.toDto(): LegalEntityStateVerboseDto {
    return LegalEntityStateVerboseDto(validFrom, validTo, type.toDto())
}

fun SiteStateDb.toDto(): SiteStateVerboseDto {
    return SiteStateVerboseDto(validFrom, validTo, type.toDto())
}

fun AddressStateDb.toDto(): AddressStateVerboseDto {
    return AddressStateVerboseDto(validFrom, validTo, type.toDto())
}

fun LogisticAddressDb.toDto(): LogisticAddressVerboseDto {
    return LogisticAddressVerboseDto(
        bpna = bpn,
        bpnLegalEntity = legalEntity?.bpn,
        bpnSite = site?.bpn,
        createdAt = createdAt,
        updatedAt = updatedAt,
        name = name,
        states = states.map { it.toDto() },
        identifiers = identifiers.map { it.toDto() },
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        confidenceCriteria = confidenceCriteria.toDto(),
        isParticipantData = legalEntity?.isCatenaXMemberData ?: site?.legalEntity?.isCatenaXMemberData ?: false,
        addressType = getAddressType(this)
    )
}

fun LogisticAddressDb.toLegalAddressResponse(): LegalAddressVerboseDto {
    return LegalAddressVerboseDto(
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        bpnLegalEntity = legalEntity?.bpn!!,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun LogisticAddressDb.toMainAddressResponse(): MainAddressVerboseDto {
    return MainAddressVerboseDto(
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        bpnSite = site?.bpn!!,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun PhysicalPostalAddressDb.toDto(): PhysicalPostalAddressVerboseDto {
    return PhysicalPostalAddressVerboseDto(
        geographicCoordinates = geographicCoordinates?.toDto(),
        countryVerbose = country.toDto(),
        postalCode = postCode,
        city = city,
        administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { RegionDto(it.countryCode, it.regionCode, it.regionName) },
        administrativeAreaLevel2 = administrativeAreaLevel2,
        administrativeAreaLevel3 = administrativeAreaLevel3,
        district = districtLevel1,
        companyPostalCode = companyPostCode,
        industrialZone = industrialZone,
        building = building,
        floor = floor,
        door = door,
        street = street?.toDto(),
        taxJurisdictionCode = taxJurisdictionCode
    )
}

fun AlternativePostalAddressDb.toDto(): AlternativePostalAddressVerboseDto {
    return AlternativePostalAddressVerboseDto(
        geographicCoordinates = geographicCoordinates?.toDto(),
        countryVerbose = country.toDto(),
        postalCode = postCode,
        city = city,
        administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { RegionDto(it.countryCode, it.regionCode, it.regionName) },
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber,
        deliveryServiceQualifier = deliveryServiceQualifier
    )
}

private fun StreetDb.toDto(): StreetDto {
    return StreetDto(
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

fun LogisticAddressDb.toMatchDto(score: Float): AddressMatchVerboseDto {
    return AddressMatchVerboseDto(score, this.toDto())
}

fun LogisticAddressDb.toCreateResponse(index: String?): AddressPartnerCreateVerboseDto {
    return AddressPartnerCreateVerboseDto(
        address = toDto(),
        index = index
    )
}

fun SiteDb.toMatchDto(): SiteMatchVerboseDto {
    return SiteMatchVerboseDto(
        mainAddress = this.mainAddress.toDto(),
        site = this.toDto(),
    )
}

fun SiteDb.toUpsertDto(entryId: String?): SitePartnerCreateVerboseDto {
    return SitePartnerCreateVerboseDto(
        site = toDto(),
        mainAddress = mainAddress.toDto(),
        index = entryId
    )
}

fun SiteDb.toDto(): SiteVerboseDto {
    return SiteVerboseDto(
        bpn,
        name,
        states = states.map { it.toDto() },
        bpnLegalEntity = legalEntity.bpn,
        confidenceCriteria = confidenceCriteria.toDto(),
        isParticipantData = legalEntity.isCatenaXMemberData,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun SiteDb.toPoolDto(): SiteWithMainAddressVerboseDto {
    return SiteWithMainAddressVerboseDto(

        site = SiteVerboseDto(
            bpn,
            name,
            states = states.map { it.toDto() },
            bpnLegalEntity = legalEntity.bpn,
            confidenceCriteria = confidenceCriteria.toDto(),
            isParticipantData = legalEntity.isCatenaXMemberData,
            createdAt = createdAt,
            updatedAt = updatedAt,
        ),
        mainAddress = mainAddress.toDto()
    )
}


fun GeographicCoordinateDb.toDto(): GeoCoordinateDto {
    return GeoCoordinateDto(longitude, latitude, altitude)
}

fun LegalEntityClassificationDb.toDto(): LegalEntityClassificationVerboseDto {
    return LegalEntityClassificationVerboseDto(value, code, type.toDto())
}

fun RelationDb.toDto(): RelationVerboseDto {
    return RelationVerboseDto(
        type = type,
        businessPartnerSourceBpnl = startNode.bpn,
        businessPartnerTargetBpnl = endNode.bpn,
        states = states.map { it.toDto() }
    )
}

fun RelationStateDb.toDto(): RelationStateVerboseDto {
    return RelationStateVerboseDto(
        validFrom = validFrom,
        validTo = validTo,
        type = type
    )
}

fun PartnerChangelogEntryDb.toDto(): ChangelogEntryVerboseDto {
    return ChangelogEntryVerboseDto(bpn, businessPartnerType, updatedAt, changelogType)
}

fun RegionDb.toRegionDto(): RegionDto {
    return RegionDto(countryCode = countryCode, regionCode = regionCode, regionName = regionName)
}

fun RegionDb.toCountrySubdivisionDto(): CountrySubdivisionDto {
    return CountrySubdivisionDto(countryCode = countryCode, code = regionCode, name = regionName)
}

fun ConfidenceCriteriaDb.toDto(): ConfidenceCriteriaDto =
    ConfidenceCriteriaDto(
        sharedByOwner,
        checkedByExternalDataSource,
        numberOfBusinessPartners,
        lastConfidenceCheckAt,
        nextConfidenceCheckAt,
        confidenceLevel
    )

fun getAddressType(logisticAddress: LogisticAddressDb): AddressType {
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