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

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.dto.response.*
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageResponse<T> {
    return PageResponse(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

fun LegalEntity.toMatchDto(score: Float): LegalEntityMatchResponse {
    return LegalEntityMatchResponse(score, this.toPoolDto())
}

fun LegalEntity.toBusinessPartnerMatchDto(score: Float): BusinessPartnerMatchResponse {
    return BusinessPartnerMatchResponse(score, this.toBusinessPartnerDto())
}

fun LegalEntity.toPoolDto(): LegalEntityPartnerResponse {
    return LegalEntityPartnerResponse(
        bpn,
        toDto(),
        currentness
    )
}

fun LegalEntity.toUpsertDto(entryId: String?): LegalEntityPartnerCreateResponse {
    return LegalEntityPartnerCreateResponse(
        bpn,
        toDto(),
        currentness,
        legalAddress.toDto(),
        entryId
    )
}

fun LegalEntity.toDto(): LegalEntityResponse {
    return LegalEntityResponse(
        identifiers.map { it.toDto() },
        names.map { it.toDto() },
        legalForm?.toDto(),
        stati.maxWithOrNull(compareBy { it.validFrom })?.toDto(),
        classification.map { it.toDto() },
        types.map { it.toDto() },
        bankAccounts.map { it.toDto() },
        roles.map { it.toDto() },
        startNodeRelations.map { it.toDto() }.plus(endNodeRelations.map { it.toDto() })
    )
}

fun LegalEntity.toBusinessPartnerDto(): BusinessPartnerResponse {
    return BusinessPartnerResponse(
        uuid = "",
        bpn = bpn,
        properties = toDto(),
        addresses = listOf(AddressPartnerResponse("", legalAddress.toDto())),
        sites = emptyList(),
        currentness = currentness
    )
}

fun Identifier.toDto(): IdentifierResponse {
    return IdentifierResponse(value, type.toDto(), issuingBody?.toDto(), status?.toDto())
}

fun IdentifierType.toDto(): TypeKeyNameUrlDto<String> {
    return TypeKeyNameUrlDto(technicalKey, name, url)
}

fun IdentifierStatus.toDto(): TypeKeyNameDto<String> {
    return TypeKeyNameDto(technicalKey, name)
}

fun IssuingBody.toDto(): TypeKeyNameUrlDto<String> {
    return TypeKeyNameUrlDto(technicalKey, name, url)
}

fun Name.toDto(): NameResponse {
    return NameResponse(value, shortName, type.toDto(), language.toDto())
}

fun LegalForm.toDto(): LegalFormResponse {
    return LegalFormResponse(technicalKey, name, url, mainAbbreviation, language.toDto(), categories.map { it.toDto() })
}

fun LegalFormCategory.toDto(): TypeNameUrlDto {
    return TypeNameUrlDto(name, url)
}

fun BusinessStatus.toDto(): BusinessStatusResponse {
    return BusinessStatusResponse(officialDenotation, validFrom, validUntil, type.toDto())
}

fun Role.toDto(): TypeKeyNameDto<String> {
    return TypeKeyNameDto(technicalKey, name)
}


fun AddressPartner.toDto(): AddressPartnerResponse {
    return AddressPartnerResponse(
        bpn,
        address.toDto()
    )
}

fun Address.toDto(): AddressResponse {
    return AddressResponse(
        version.toDto(),
        careOf,
        contexts,
        country.toDto(),
        administrativeAreas.map { it.toDto() },
        postCodes.map { it.toDto() },
        localities.map { it.toDto() },
        thoroughfares.map { it.toDto() },
        premises.map { it.toDto() },
        postalDeliveryPoints.map { it.toDto() },
        geoCoordinates?.toDto(),
        types.map { it.toDto() })
}

fun Address.toLegalSearchResponse(bpnL: String): LegalAddressSearchResponse {
    return LegalAddressSearchResponse(
        bpnL,
        this.toDto()
    )
}

fun Address.toMainSearchResponse(bpnS: String): MainAddressSearchResponse {
    return MainAddressSearchResponse(
        bpnS,
        this.toDto()
    )
}

fun AddressPartner.toMatchDto(score: Float): AddressMatchResponse {
    return AddressMatchResponse(score, this.toDtoWithReference())
}

fun AddressPartner.toPoolDto(): AddressPartnerResponse {
    return AddressPartnerResponse(
        bpn,
        address.toDto()
    )
}

fun AddressPartner.toCreateResponse(index: String?): AddressPartnerCreateResponse {
    return AddressPartnerCreateResponse(
        bpn,
        address.toDto(),
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

fun Site.toDto(): SitePartnerResponse {
    return SitePartnerResponse(
        bpn,
        name
    )
}

fun Site.toWithReferenceDto(): SitePartnerSearchResponse {
    return SitePartnerSearchResponse(
        toDto(),
        legalEntity.bpn
    )
}

fun AdministrativeArea.toDto(): AdministrativeAreaResponse {
    return AdministrativeAreaResponse(value, shortName, fipsCode, type.toDto(), language.toDto())
}


fun PostCode.toDto(): PostCodeResponse {
    return PostCodeResponse(value, type.toDto())
}

fun Locality.toDto(): LocalityResponse {
    return LocalityResponse(value, shortName, localityType.toDto(), language.toDto())
}

fun Thoroughfare.toDto(): ThoroughfareResponse {
    return ThoroughfareResponse(value, name, shortName, number, direction, type.toDto(), language.toDto())
}

fun Premise.toDto(): PremiseResponse {
    return PremiseResponse(value, shortName, number, type.toDto(), language.toDto())
}

fun PostalDeliveryPoint.toDto(): PostalDeliveryPointResponse {
    return PostalDeliveryPointResponse(value, shortName, number, type.toDto(), language.toDto())
}

fun AddressVersion.toDto(): AddressVersionResponse {
    return AddressVersionResponse(characterSet.toDto(), language.toDto())
}

fun GeographicCoordinate.toDto(): GeoCoordinateDto {
    return GeoCoordinateDto(longitude, latitude, altitude)
}

fun Classification.toDto(): ClassificationResponse {
    return ClassificationResponse(value, code, type?.toDto())
}

fun ClassificationType.toDto(): TypeNameUrlDto {
    return TypeNameUrlDto(name, url)
}

fun Relation.toDto(): RelationResponse {
    return RelationResponse(relationClass.toDto(), type.toDto(), startNode.bpn, endNode.bpn, startedAt, endedAt)
}

fun BankAccount.toDto(): BankAccountResponse {
    return BankAccountResponse(
        trustScores, currency.toDto(), internationalBankAccountIdentifier, internationalBankIdentifier,
        nationalBankAccountIdentifier, nationalBankIdentifier
    )
}

fun SyncRecord.toDto(): SyncResponse {
    return SyncResponse(type, status, count, progress, errorDetails, startedAt, finishedAt)
}

fun PartnerChangelogEntry.toDto(): ChangelogEntryResponse {
    return ChangelogEntryResponse(bpn, changelogType, createdAt)
}
