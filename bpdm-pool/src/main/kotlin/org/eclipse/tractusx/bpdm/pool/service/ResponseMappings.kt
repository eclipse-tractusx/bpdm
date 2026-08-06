/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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


fun <S: Any, T> Page<S>.toDto(dtoContent: Collection<T>): PageDto<T> {
    return PageDto(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

fun <S: Any, T> Page<S>.toDto(map: (S) -> T): PageDto<T> {
    return PageDto(totalElements, totalPages, number, numberOfElements, content.map { map(it) })
}

fun LegalEntityDb.toUpsertDto(entryId: String?): LegalEntityPartnerCreateVerboseDto {
    return LegalEntityPartnerCreateVerboseDto(
        legalEntity = toLegalEntityWithLegalAddress(),
        index = entryId
    )
}

fun LegalEntityDb.toLegalEntityWithLegalAddress(): LegalEntityWithLegalAddressVerboseDto {
    return LegalEntityWithLegalAddressVerboseDto(
        legalAddress = legalAddress.toInvariantDto(),
        header = toDto(),
        scriptVariants = scriptVariants.toLegalEntityScriptVariants(legalAddress.scriptVariants),
    )
}

fun LegalEntityDb.toDto(): LegalEntityHeaderVerboseDto {
    return LegalEntityHeaderVerboseDto(
        bpnl = bpn,
        legalName = legalName.value,
        legalShortName = legalName.shortName,
        legalFormVerbose = legalForm?.toDto(),
        identifiers = identifiers.map { it.toDto() },
        states = states.map { it.toDto() },
        relations = startNodeRelations.plus(endNodeRelations).map { it.toDto() },
        currentness = currentness,
        confidenceCriteria = confidenceCriteria.toDto(),
        isParticipantData = isDataSpaceParticipant,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ownershipUltimate = ownershipUltimate,
        ultimateOwnerBpnl = ultimateOwnerBpnl
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

fun LogisticAddressDb.toUpdateDto(): AddressPartnerUpdateVerboseDto{
    return AddressPartnerUpdateVerboseDto(
        address = toInvariantDto(),
        scriptVariants = scriptVariants.map { it.toLogisticAddressScriptVariant() }
    )
}

fun LogisticAddressDb.toInvariantDto(): LogisticAddressInvariantVerboseDto {
    return LogisticAddressInvariantVerboseDto(
        bpna = bpn,
        bpnLegalEntity = legalEntity?.bpn,
        bpnSite = mainSite?.bpn,
        additionalSites = additionalSites.map { it.bpn },
        createdAt = createdAt,
        updatedAt = updatedAt,
        name = name,
        states = states.map { it.toDto() },
        identifiers = identifiers.map { it.toDto() },
        relations = startAddressRelations.plus(endAddressRelations).map { it.toDto() },
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        confidenceCriteria = confidenceCriteria.toDto(),
        isParticipantData = legalEntity?.isDataSpaceParticipant ?: mainSite?.legalEntity?.isDataSpaceParticipant ?: false,
        addressType = getAddressType(this)
    )
}

fun LogisticAddressDb.toDto(): LogisticAddressVerboseDto {
    return LogisticAddressVerboseDto(
        address = toInvariantDto(),
        scriptVariants = scriptVariants.map { it.toLogisticAddressScriptVariant() }
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

fun LogisticAddressDb.toCreateResponse(index: String?): AddressPartnerCreateVerboseDto {
    return AddressPartnerCreateVerboseDto(
        address = toInvariantDto(),
        scriptVariants = scriptVariants.map { it.toLogisticAddressScriptVariant() },
        index = index
    )
}

fun SiteDb.toUpsertDto(entryId: String?): SitePartnerCreateVerboseDto {
    return SitePartnerCreateVerboseDto(
        site = toDto(),
        mainAddress = mainAddress.toInvariantDto(),
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
        isParticipantData = legalEntity.isDataSpaceParticipant,
        scriptVariants = toSiteScriptVariants(),
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
            isParticipantData = legalEntity.isDataSpaceParticipant,
            scriptVariants = toSiteScriptVariants(),
            createdAt = createdAt,
            updatedAt = updatedAt,
        ),
        mainAddress = mainAddress.toInvariantDto()
    )
}


fun GeographicCoordinateDb.toDto(): GeoCoordinateDto {
    return GeoCoordinateDto(longitude, latitude, altitude)
}

fun RelationDb.toDto(): RelationVerboseDto {
    return RelationVerboseDto(
        type = type,
        businessPartnerSourceBpnl = startNode.bpn,
        businessPartnerTargetBpnl = endNode.bpn,
        validityPeriods = validityPeriods.sortedBy { it.validFrom }.map { it.toDto() },
        reasonCode = reasonCode?.technicalKey
    )
}

fun AddressRelationDb.toDto(): AddressRelationVerboseDto {
    return AddressRelationVerboseDto(
        type = type,
        businessPartnerSourceBpna = startAddress.bpn,
        businessPartnerTargetBpna = endAddress.bpn,
        validityPeriods = validityPeriods.sortedBy { it.validFrom }.map { it.toDto() },
        reasonCode = reasonCode?.technicalKey
    )
}

fun RelationValidityPeriodDb.toDto(): RelationValidityPeriod {
    return RelationValidityPeriod(
        validFrom = validFrom,
        validTo = validTo,
    )
}

fun PartnerChangelogEntryDb.toDto(): ChangelogEntryVerboseDto {
    return ChangelogEntryVerboseDto(bpn, businessPartnerType, updatedAt, changelogType)
}

fun RegionDb.toCountrySubdivisionDto(): CountrySubdivisionDto {
    return CountrySubdivisionDto(countryCode = countryCode, code = regionCode, name = regionName)
}

fun ConfidenceCriteriaDb.toDto(): ConfidenceCriteriaDto =
    ConfidenceCriteriaDto(
        sharedByOwner,
        checkedByExternalDataSource,
        numberOfSharingMembers,
        lastConfidenceCheckAt,
        nextConfidenceCheckAt,
        confidenceLevel
    )

/**
 * The site rendered as the API's `bpnSite` for backward compatibility: the oldest member by `createdAt`, which for
 * pre-existing data is the address's former single site. The remaining members are exposed as [additionalSites].
 */
val LogisticAddressDb.mainSite: SiteDb?
    get() = sites.minByOrNull { it.createdAt }

val LogisticAddressDb.additionalSites: List<SiteDb>
    get() = sites.sortedBy { it.createdAt }.drop(1)

/** An address is a site's main address iff one of the sites it belongs to has it as its main address. */
private fun LogisticAddressDb.isSiteMainAddress() = sites.any { it.mainAddress == this }

fun getAddressType(logisticAddress: LogisticAddressDb): AddressType {
    return when {
        logisticAddress.legalEntity?.legalAddress == logisticAddress &&
                logisticAddress.isSiteMainAddress() -> AddressType.LegalAndSiteMainAddress

        logisticAddress.legalEntity?.legalAddress != logisticAddress &&
                !logisticAddress.isSiteMainAddress() -> AddressType.AdditionalAddress

        logisticAddress.legalEntity?.legalAddress == logisticAddress -> AddressType.LegalAddress

        logisticAddress.isSiteMainAddress() -> AddressType.SiteMainAddress

        else -> throw IllegalStateException("Unable to determine address type.")
    }
}

private fun List<LegalEntityScriptVariantDb>.toLegalEntityScriptVariants(legalAddressVariants: List<LogisticAddressScriptVariantDb>): List<LegalEntityScriptVariantDto>{
    val legalAddressVariantsByCode = legalAddressVariants.associateBy { it.scriptCode.technicalKey }

    return map { variant ->
        val scriptCode = variant.scriptCode.technicalKey
        // The legal address covers every script its legal entity is named in: the parsers reject a variant it does not
        // cover and ScriptVariantCoverageService prunes any the legal address stops covering.
        val legalAddressVariant = legalAddressVariantsByCode[scriptCode]
            ?: throw IllegalStateException("Legal entity script variant of script code '$scriptCode' is not covered by the legal address.")
        LegalEntityScriptVariantDto(scriptCode, variant.legalName, variant.shortName, legalAddressVariant.toDto())
    }
}

private fun SiteDb.toSiteScriptVariants(): List<SiteScriptVariantDto>{
    return scriptVariants.toSiteScriptVariants(mainAddress.scriptVariants)
}

private fun List<SiteScriptVariantDb>.toSiteScriptVariants(mainAddressVariants: List<LogisticAddressScriptVariantDb>): List<SiteScriptVariantDto>{
    val mainAddressVariantsByCode = mainAddressVariants.associateBy { it.scriptCode.technicalKey }

    return map { variant ->
        val scriptCode = variant.scriptCode.technicalKey
        val mainAddressVariant = mainAddressVariantsByCode[scriptCode]
            ?: throw IllegalStateException("Site script variant of script code '$scriptCode' is not covered by the main address.")
        SiteScriptVariantDto(scriptCode, variant.name, mainAddressVariant.toDto())
    }
}

private fun LogisticAddressScriptVariantDb.toLogisticAddressScriptVariant(): LogisticAddressScriptVariantDto{
    return LogisticAddressScriptVariantDto(scriptCode.technicalKey, toDto())
}

private fun LogisticAddressScriptVariantDb.toDto(): PostalAddressScriptVariantDto{
    return PostalAddressScriptVariantDto(name, physicalAddress.toDto(), alternativeAddress?.toDto())
}

private fun PhysicalAddressScriptVariantDb.toDto(): PhysicalAddressScriptVariantDto{
    return PhysicalAddressScriptVariantDto(
        city = city,
        district = district,
        street = street?.toDto(),
        industrialZone = industrialZone,
        building = building,
        floor = floor,
        door = door
    )
}

private fun AlternativeAddressScriptVariantDb.toDto(): AlternativeAddressScriptVariantDto{
    return AlternativeAddressScriptVariantDto(city)
}

private fun StreetScriptVariantDb.toDto(): StreetScriptVariantDto{
    return StreetScriptVariantDto(
        name = name,
        direction = direction,
        namePrefix = namePrefix,
        additionalNamePrefix = additionalNamePrefix,
        nameSuffix = nameSuffix,
        additionalNameSuffix = additionalNameSuffix
    )
}
