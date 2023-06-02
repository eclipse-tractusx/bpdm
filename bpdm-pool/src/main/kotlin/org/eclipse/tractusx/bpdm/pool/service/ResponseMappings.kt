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
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageResponse<T> {
    return PageResponse(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

fun LegalEntity.toMatchDto(score: Float): LegalEntityMatchResponse {
    return LegalEntityMatchResponse(
        score = score,
        legalEntity = this.toDto(),
        legalName = this.legalName.value,
        legalAddress = legalAddress.toDto(),
    )
}

fun LegalEntity.toBusinessPartnerMatchDto(score: Float): BusinessPartnerMatchResponse {
    return BusinessPartnerMatchResponse(score, this.toBusinessPartnerDto())
}

fun LegalEntity.toUpsertDto(entryId: String?): LegalEntityPartnerCreateResponse {
    return LegalEntityPartnerCreateResponse(
        legalEntity = toDto(),
        legalAddress = legalAddress.toDto(),
        index = entryId,
        legalName = legalName.value,
    )
}

fun LegalEntity.toPoolLegalEntity(): PoolLegalEntityResponse {
    return PoolLegalEntityResponse(
        legalName = legalName.value,
        legalAddress = legalAddress.toDto(),
        legalEntity = LegalEntityResponse(
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

fun LegalEntity.toDto(): LegalEntityResponse {
    return LegalEntityResponse(
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

fun LegalEntity.toBusinessPartnerDto(): BusinessPartnerResponse {
    return BusinessPartnerResponse(
        uuid = "",
        legalEntity = toDto(),
        addresses = listOf(legalAddress.toDto()),
        legalAddress = legalAddress.toDto(),
        sites = emptyList(),
        legalName = legalName.value,
    )
}

fun LegalEntityIdentifier.toDto(): LegalEntityIdentifierResponse {
    return LegalEntityIdentifierResponse(value, type.toTypeKeyNameDto(), issuingBody)
}

fun AddressIdentifier.toDto(): AddressIdentifierResponse {
    return AddressIdentifierResponse(value, type.toTypeKeyNameDto())
}

fun IdentifierType.toTypeKeyNameDto(): TypeKeyNameDto<String> {
    return TypeKeyNameDto(technicalKey, name)
}

fun IdentifierType.toDto(): IdentifierTypeDto {
    return IdentifierTypeDto(technicalKey, lsaType, name,
        details.map { IdentifierTypeDetailDto(it.countryCode, it.mandatory) })
}

fun LegalForm.toDto(): LegalFormResponse {
    return LegalFormResponse(technicalKey, name, abbreviation)
}

fun LegalEntityState.toDto(): LegalEntityStateResponse {
    return LegalEntityStateResponse(officialDenotation, validFrom, validTo, type.toDto())
}

fun SiteState.toDto(): SiteStateResponse {
    return SiteStateResponse(description, validFrom, validTo, type.toDto())
}

fun AddressState.toDto(): AddressStateResponse {
    return AddressStateResponse(description, validFrom, validTo, type.toDto())
}

fun LogisticAddress.toDto(): LogisticAddressResponse {
    return LogisticAddressResponse(
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

fun LogisticAddress.toLegalAddressResponse(): LegalAddressResponse {
    return LegalAddressResponse(
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        bpnLegalEntity = legalEntity?.bpn!!,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun LogisticAddress.toMainAddressResponse(): MainAddressResponse {
    return MainAddressResponse(
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        bpnSite = site?.bpn!!,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun PhysicalPostalAddress.toDto(): PhysicalPostalAddressResponse {
    return PhysicalPostalAddressResponse(
        baseAddress = BasePostalAddressResponse(
            geographicCoordinates = geographicCoordinates?.toDto(),
            country = country.toDto(),
            postalCode = postCode,
            city = city,
        ),
        street = street?.toDto(),
        areaPart = AreaDistrictResponse(
            administrativeAreaLevel1 = administrativeAreaLevel1?.let { NameRegioncodeDto(it.regionName, it.regionCode) },
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

fun AlternativePostalAddress.toDto(): AlternativePostalAddressResponse {
    return AlternativePostalAddressResponse(
        baseAddress = BasePostalAddressResponse(
            geographicCoordinates = geographicCoordinates?.toDto(),
            country = country.toDto(),
            postalCode = postCode,
            city = city,
        ),
        areaPart = AreaDistrictAlternativResponse(
            administrativeAreaLevel1 = administrativeAreaLevel1?.let { NameRegioncodeDto(it.regionName, it.regionCode) },
        ),
        type = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber
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

fun LogisticAddress.toMatchDto(score: Float): AddressMatchResponse {
    return AddressMatchResponse(score, this.toDto())
}

fun LogisticAddress.toCreateResponse(index: String?): AddressPartnerCreateResponse {
    return AddressPartnerCreateResponse(
        address = toDto(),
        index = index
    )
}

fun Site.toUpsertDto(entryId: String?): SitePartnerCreateResponse {
    return SitePartnerCreateResponse(
        site = toDto(),
        mainAddress = mainAddress.toDto(),
        index = entryId
    )
}

fun Site.toDto(): SiteResponse {
    return SiteResponse(
        bpn,
        name,
        states = states.map { it.toDto() },
        bpnLegalEntity = legalEntity.bpn,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}


fun GeographicCoordinate.toDto(): GeoCoordinateDto {
    return GeoCoordinateDto(longitude, latitude, altitude)
}

fun Classification.toDto(): ClassificationResponse {
    return ClassificationResponse(value, code, type.toDto())
}

fun Relation.toDto(): RelationResponse {
    return RelationResponse(
        type = type.toDto(),
        startBpn = startNode.bpn,
        endBpn = endNode.bpn,
        validFrom = validFrom,
        validTo = validTo
    )
}

fun SyncRecord.toDto(): SyncResponse {
    return SyncResponse(type, status, count, progress, errorDetails, startedAt, finishedAt)
}

fun PartnerChangelogEntry.toDto(): ChangelogEntryResponse {
    return ChangelogEntryResponse(bpn, changelogType, createdAt)
}
