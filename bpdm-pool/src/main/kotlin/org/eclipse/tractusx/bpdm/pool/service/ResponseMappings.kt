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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.IdentifierTypeDetailDto
import org.eclipse.tractusx.bpdm.common.dto.IdentifierTypeDto
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
    return LegalEntityMatchResponse(score, this.toDto())
}

fun LegalEntity.toBusinessPartnerMatchDto(score: Float): BusinessPartnerMatchResponse {
    return BusinessPartnerMatchResponse(score, this.toBusinessPartnerDto())
}

fun LegalEntity.toUpsertDto(entryId: String?): LegalEntityPartnerCreateResponse {
    return LegalEntityPartnerCreateResponse(
        // TODO Mapping
        legalEntity = toDto(),
        legalAddress = LogisticAddressResponse(bpn = "TODO", postalAddress = PostalAddressResponse(country = TypeKeyNameDto(CountryCode.DE,CountryCode.DE.name), city="TODO")),
        index = entryId
    )
}

fun LegalEntity.toDto(): LegalEntityResponse {
    return LegalEntityResponse(
        bpn = bpn,
        identifiers = identifiers.map { it.toDto() },
        legalName = legalName.toDto(),
        legalForm = legalForm?.toDto(),
        states = states.map { it.toLegalEntityStatusDto() },
        classifications = classifications.map { it.toDto() },
        relations = startNodeRelations.plus(endNodeRelations).map { it.toDto() },
        currentness = currentness,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun LegalEntity.toBusinessPartnerDto(): BusinessPartnerResponse {
    return BusinessPartnerResponse(
        //TODO Mapping
        uuid = "",
        legalEntity = toDto(),
        addresses = listOf(LogisticAddressResponse("", PostalAddressResponse(country = TypeKeyNameDto(CountryCode.DE,CountryCode.DE.name), city="TODO"),)),
        sites = emptyList(),
    )
}

fun LegalEntityIdentifier.toDto(): LegalEntityIdentifierResponse {
    return LegalEntityIdentifierResponse(value, type.toTypeKeyNameDto(), issuingBody)
}

// TODO still unused!
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

fun Name.toDto(): NameResponse {
    return NameResponse(value, shortName)
}

fun LegalForm.toDto(): LegalFormResponse {
    return LegalFormResponse(technicalKey, name, abbreviation)
}

fun LegalEntityState.toLegalEntityStatusDto(): LegalEntityStateResponse {
    return LegalEntityStateResponse(officialDenotation, validFrom, validTo, type.toDto())
}

fun SiteState.toSiteStatusDto(): SiteStateResponse {
    return SiteStateResponse(description, validFrom, validTo, type.toDto())
}

fun AddressPartner.toDto(): LogisticAddressResponse {
    return LogisticAddressResponse(
        //TODO mapping
        bpn,
        PostalAddressResponse(country = TypeKeyNameDto(CountryCode.DE,CountryCode.DE.name), city="TODO"),
    )
}

fun Address.toDto(): LogisticAddressResponse {
    return LogisticAddressResponse(

        // TODO mapping
        bpn = "TODO",
        postalAddress = PostalAddressResponse(country = TypeKeyNameDto(CountryCode.DE,CountryCode.DE.name), city="TODO")
        )
}

fun Address.toLegalSearchResponse(bpnL: String): LegalAddressSearchResponse {
    return LegalAddressSearchResponse(
        // TODO mapping
        bpnL,
        legalAddress = LogisticAddressResponse(bpn = "TODO", PostalAddressResponse(country = TypeKeyNameDto(CountryCode.DE,CountryCode.DE.name), city="TODO"))
    )
}

fun Address.toMainSearchResponse(bpnS: String): MainAddressSearchResponse {
    return MainAddressSearchResponse(
        //TODO
        site = bpnS,
        mainAddress = LogisticAddressResponse(bpn = "TODO", PostalAddressResponse(city="TODO", country = this.country.toDto()))
    )
}

fun AddressPartner.toMatchDto(score: Float): AddressMatchResponse {
    return AddressMatchResponse(score, this.toDtoWithReference())
}

fun AddressPartner.toPoolDto(): LogisticAddressResponse {
    return LogisticAddressResponse(
        //TODO mapping
        bpn,
        PostalAddressResponse(country = TypeKeyNameDto(CountryCode.DE,CountryCode.DE.name), city="TODO")
    )
}

fun AddressPartner.toCreateResponse(index: String?): AddressPartnerCreateResponse {
    return AddressPartnerCreateResponse(
        //TODO mapping
        bpn,
        PostalAddressResponse(country = TypeKeyNameDto(CountryCode.DE,CountryCode.DE.name), city="TODO"),
        index
    )
}

fun AddressPartner.toDtoWithReference(): AddressPartnerSearchResponse {
    return AddressPartnerSearchResponse(
        toDto(),
        legalEntity?.bpn,
        site?.bpn
    )
}


fun Site.toUpsertDto(entryId: String?): SitePartnerCreateResponse {
    return SitePartnerCreateResponse(
        bpn,
        name,
        mainAddress.toDto(),
        entryId
    )
}

fun Site.toDto(): SiteResponse {
    return SiteResponse(
        bpn,
        name,
        states = states.map { it.toSiteStatusDto() },
        bpnLegalEntity = legalEntity.bpn,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}


fun AdministrativeArea.toDto(): AdministrativeAreaResponse {
    return AdministrativeAreaResponse(value, shortName, fipsCode, type.toDto(), language.toDto())
}

fun PostCode.toDto(): PostCodeResponse {
    return PostCodeResponse(value, type.toDto())
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
