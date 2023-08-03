/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageDto<T> {
    return PageDto(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

fun LegalEntity.toMatchDto(score: Float): LegalEntityMatchVerboseDto {
    return LegalEntityMatchVerboseDto(
        score = score,
        legalEntity = this.toDto(),
        legalName = this.legalName.value,
        legalAddress = legalAddress.toDto(),
    )
}

fun LegalEntity.toUpsertDto(entryId: String?): LegalEntityPartnerCreateVerboseDto {
    return LegalEntityPartnerCreateVerboseDto(
        legalEntity = toDto(),
        legalAddress = legalAddress.toDto(),
        index = entryId,
        legalName = legalName.value,
    )
}

fun LegalEntity.toPoolLegalEntity(): PoolLegalEntityVerboseDto {
    return PoolLegalEntityVerboseDto(
        legalName = legalName.value,
        legalAddress = legalAddress.toDto(),
        legalEntity = LegalEntityVerboseDto(
            bpnl = bpn,
            identifiers = identifiers.map { it.toDto() },
            legalShortName = legalName.shortName,
            legalForm = legalForm?.toDto(),
            states = states.map { it.toDto() },
            classifications = classifications.map { it.toDto() },
            relations = startNodeRelations.plus(endNodeRelations).map { it.toDto() },
            currentness = currentness,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    )
}

fun LegalEntity.toDto(): LegalEntityVerboseDto {
    return LegalEntityVerboseDto(
        bpnl = bpn,
        identifiers = identifiers.map { it.toDto() },
        legalShortName = legalName.shortName,
        legalForm = legalForm?.toDto(),
        states = states.map { it.toDto() },
        classifications = classifications.map { it.toDto() },
        relations = startNodeRelations.plus(endNodeRelations).map { it.toDto() },
        currentness = currentness,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun LegalEntityIdentifier.toDto(): LegalEntityIdentifierVerboseDto {
    return LegalEntityIdentifierVerboseDto(value, type.toTypeKeyNameDto(), issuingBody)
}

fun AddressIdentifier.toDto(): AddressIdentifierVerboseDto {
    return AddressIdentifierVerboseDto(value, type.toTypeKeyNameDto())
}

fun IdentifierType.toTypeKeyNameDto(): TypeKeyNameVerboseDto<String> {
    return TypeKeyNameVerboseDto(technicalKey, name)
}

fun IdentifierType.toDto(): IdentifierTypeDto {
    return IdentifierTypeDto(technicalKey, businessPartnerType, name,
        details.map { IdentifierTypeDetailDto(it.countryCode, it.mandatory) })
}

fun LegalForm.toDto(): LegalFormDto {
    return LegalFormDto(technicalKey, name, abbreviation)
}

fun LegalEntityState.toDto(): LegalEntityStateVerboseDto {
    return LegalEntityStateVerboseDto(description, validFrom, validTo, type.toDto())
}

fun SiteState.toDto(): SiteStateVerboseDto {
    return SiteStateVerboseDto(description, validFrom, validTo, type.toDto())
}

fun AddressState.toDto(): AddressStateVerboseDto {
    return AddressStateVerboseDto(description, validFrom, validTo, type.toDto())
}

fun LogisticAddress.toDto(): LogisticAddressVerboseDto {
    return LogisticAddressVerboseDto(
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
        alternativePostalAddress = alternativePostalAddress?.toDto()
    )
}

fun LogisticAddress.toLegalAddressResponse(): LegalAddressVerboseDto {
    return LegalAddressVerboseDto(
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        bpnLegalEntity = legalEntity?.bpn!!,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun LogisticAddress.toMainAddressResponse(): MainAddressVerboseDto {
    return MainAddressVerboseDto(
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        bpnSite = site?.bpn!!,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun PhysicalPostalAddress.toDto(): PhysicalPostalAddressVerboseDto {
    return PhysicalPostalAddressVerboseDto(
        baseAddress = BasePostalAddressVerboseDto(
            geographicCoordinates = geographicCoordinates?.toDto(),
            country = country.toDto(),
            postalCode = postCode,
            city = city,
        ),
        street = street?.toDto(),
        areaPart = AreaDistrictVerboseDto(
            administrativeAreaLevel1 = administrativeAreaLevel1?.let { RegionDto(it.countryCode, it.regionCode, it.regionName) },
            administrativeAreaLevel2 = administrativeAreaLevel2,
            administrativeAreaLevel3 = administrativeAreaLevel3,
            district = districtLevel1,
        ),
        basePhysicalAddress = BasePhysicalAddressDto(
            companyPostalCode = companyPostCode,
            industrialZone = industrialZone,
            building = building,
            floor = floor,
            door = door
        )
    )
}

fun AlternativePostalAddress.toDto(): AlternativePostalAddressVerboseDto {
    return AlternativePostalAddressVerboseDto(
        baseAddress = BasePostalAddressVerboseDto(
            geographicCoordinates = geographicCoordinates?.toDto(),
            country = country.toDto(),
            postalCode = postCode,
            city = city,
        ),
        areaPart = AreaDistrictAlternativVerboseDto(
            administrativeAreaLevel1 = administrativeAreaLevel1?.let { RegionDto(it.countryCode, it.regionCode, it.regionName) },
        ),
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber,
        deliveryServiceQualifier = deliveryServiceQualifier
    )
}

private fun Street.toDto(): StreetDto {
    return StreetDto(
        name = name,
        houseNumber = houseNumber,
        milestone = milestone,
        direction = direction
    )
}

fun LogisticAddress.toMatchDto(score: Float): AddressMatchVerboseDto {
    return AddressMatchVerboseDto(score, this.toDto())
}

fun LogisticAddress.toCreateResponse(index: String?): AddressPartnerCreateVerboseDto {
    return AddressPartnerCreateVerboseDto(
        address = toDto(),
        index = index
    )
}

fun Site.toMatchDto(): SiteMatchVerboseDto {
    return SiteMatchVerboseDto(
        mainAddress = this.mainAddress.toDto(),
        site = this.toDto(),
    )
}

fun Site.toUpsertDto(entryId: String?): SitePartnerCreateVerboseDto {
    return SitePartnerCreateVerboseDto(
        site = toDto(),
        mainAddress = mainAddress.toDto(),
        index = entryId
    )
}

fun Site.toDto(): SiteVerboseDto {
    return SiteVerboseDto(
        bpn,
        name,
        states = states.map { it.toDto() },
        bpnLegalEntity = legalEntity.bpn,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun Site.toPoolDto(): SitePoolVerboseDto {
    return SitePoolVerboseDto(

        site = SiteVerboseDto(
            bpn,
            name,
            states = states.map { it.toDto() },
            bpnLegalEntity = legalEntity.bpn,
            createdAt = createdAt,
            updatedAt = updatedAt,
        ),
        mainAddress = mainAddress.toDto()
    )
}


fun GeographicCoordinate.toDto(): GeoCoordinateDto {
    return GeoCoordinateDto(longitude, latitude, altitude)
}

fun Classification.toDto(): ClassificationVerboseDto {
    return ClassificationVerboseDto(value, code, type.toDto())
}

fun Relation.toDto(): RelationVerboseDto {
    return RelationVerboseDto(
        type = type.toDto(),
        startBpnl = startNode.bpn,
        endBpnl = endNode.bpn,
        validFrom = validFrom,
        validTo = validTo
    )
}

fun SyncRecord.toDto(): SyncDto {
    return SyncDto(type, status, count, progress, errorDetails, startedAt, finishedAt)
}

fun PartnerChangelogEntry.toDto(): ChangelogEntryVerboseDto {
    return ChangelogEntryVerboseDto(bpn, changelogType, createdAt, businessPartnerType)
}

fun Region.toDto(): RegionDto {
    return RegionDto(countryCode, regionCode, regionName)
}
