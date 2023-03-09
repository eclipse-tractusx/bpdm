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

package org.eclipse.tractusx.bpdm.common.service

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.CurrencyCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.common.exception.BpdmMappingException
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.HasDefaultValue

object SaasMappings {

    const val BPN_TECHNICAL_KEY = "CX_BPN"

    fun findBpn(identifiers: Collection<IdentifierSaas>): String? {
        return identifiers.find { it.type?.technicalKey == BPN_TECHNICAL_KEY }?.value
    }

    private fun toReference(type: TypeKeyNameUrlSaas?): String {
        return type!!.technicalKey!!
    }

    private fun toOptionalReference(type: TypeKeyNameUrlSaas?): String? {
        return type?.technicalKey
    }

    private fun toOptionalReference(type: TypeKeyNameSaas?): String? {
        return type?.technicalKey
    }

    fun toOptionalReference(legalForm: LegalFormSaas?): String? {
        return legalForm?.technicalKey
    }

    private inline fun <reified T> toType(type: TypeKeyNameUrlSaas): T where T : Enum<T> {
        return enumValueOf(type.technicalKey!!)
    }

    inline fun <reified T> toTypeOrDefault(type: TypeKeyNameUrlSaas?): T where T : Enum<T>, T : HasDefaultValue<T> {
        return technicalKeyToType(type?.technicalKey)
    }

    inline fun <reified T> technicalKeyToType(technicalKey: String?): T where T : Enum<T>, T : HasDefaultValue<T> {
        val allValues = enumValues<T>()
        val foundValue = if (technicalKey != null) allValues.map { it.name }.find { technicalKey == it } else null
        return if (foundValue != null) enumValueOf(foundValue) else allValues.first().getDefault()
    }

    fun toLanguageCode(language: LanguageSaas?): LanguageCode {
        return language?.technicalKey ?: LanguageCode.undefined
    }

    inline fun <reified T> toTypeOrDefault(type: TypeKeyNameSaas?): T where T : Enum<T>, T : HasDefaultValue<T> {
        return technicalKeyToType(type?.technicalKey)
    }

    fun toCountryCode(country: CountrySaas?): CountryCode {
        return country?.shortName ?: CountryCode.UNDEFINED
    }

    fun BusinessPartnerSaas.toLegalEntityDto(): LegalEntityDto {
        return LegalEntityDto(
            identifiers = identifiers.filter { it.type?.technicalKey != BPN_TECHNICAL_KEY }.map { toDto(it) },
            names = names.map { toDto(it) },
            legalForm = toOptionalReference(legalForm),
            status = if (status != null) toDto(status) else null,
            profileClassifications = toDto(profile),
            bankAccounts = bankAccounts.map { toDto(it) },
            legalAddress = toDto(addresses.firstOrNull() ?: throw BpdmMappingException(this::class, LegalEntityDto::class, "No legal address", id ?: "Unknown"))
        )
    }

    fun BusinessPartnerSaas.toSiteDto(): SiteDto {
        return SiteDto(
            name = names.first().value,
            mainAddress = toDto(addresses.first())
        )
    }

    fun toDto(identifier: IdentifierSaas): IdentifierDto {
        return IdentifierDto(
            identifier.value ?: throw BpdmNullMappingException(IdentifierSaas::class, IdentifierDto::class, IdentifierSaas::value),
            toReference(identifier.type),
            toOptionalReference(identifier.issuingBody),
            toOptionalReference(identifier.status)
        )
    }

    fun toDto(name: NameSaas): NameDto {
        return NameDto(
            name.value,
            name.shortName,
            toTypeOrDefault(name.type),
            toLanguageCode(name.language)
        )
    }

    fun toDto(status: BusinessPartnerStatusSaas): BusinessStatusDto {
        return BusinessStatusDto(
            officialDenotation = status.officialDenotation,
            validFrom = status.validFrom,
            validUntil = status.validUntil,
            type = toTypeOrDefault(status.type)
        )
    }

    fun toDto(profile: PartnerProfileSaas?): Collection<ClassificationDto> {
        return profile?.classifications?.map { toDto(it) } ?: emptyList()
    }

    fun toDto(classification: ClassificationSaas): ClassificationDto {
        return ClassificationDto(classification.value, classification.code, toType<ClassificationType>(classification.type!!))
    }

    fun toDto(account: BankAccountSaas): BankAccountDto {
        return BankAccountDto(
            emptyList(),
            CurrencyCode.UNDEFINED,
            account.internationalBankAccountIdentifier,
            account.internationalBankIdentifier,
            account.nationalBankAccountIdentifier,
            account.nationalBankIdentifier
        )
    }

    fun toDto(address: AddressSaas): AddressDto {
        return AddressDto(
            toDto(address.version),
            address.careOf?.value,
            address.contexts.mapNotNull { it.value },
            toCountryCode(address.country),
            address.administrativeAreas.map { toDto(it) },
            address.postCodes.map { toDto(it) },
            address.localities.map { toDto(it) },
            address.thoroughfares.map { toDto(it) },
            address.premises.map { toDto(it) },
            address.postalDeliveryPoints.map { toDto(it) },
            if (address.geographicCoordinates != null) toDto(address.geographicCoordinates) else null,
            address.types.map { toTypeOrDefault(it) }
        )
    }

    fun toDto(version: AddressVersionSaas?): AddressVersionDto {
        return AddressVersionDto(toTypeOrDefault(version?.characterSet), toLanguageCode(version?.language))
    }

    fun toDto(area: AdministrativeAreaSaas): AdministrativeAreaDto {
        return AdministrativeAreaDto(
            area.value ?: throw BpdmNullMappingException(area::class, AdministrativeAreaDto::class, area::value),
            area.shortName,
            null,
            toTypeOrDefault(area.type)
        )
    }

    fun toDto(postcode: PostCodeSaas): PostCodeDto {
        return PostCodeDto(
            postcode.value ?: throw BpdmNullMappingException(postcode::class, PostCodeDto::class, postcode::value),
            toTypeOrDefault(postcode.type)
        )
    }

    fun toDto(locality: LocalitySaas): LocalityDto {
        return LocalityDto(
            locality.value ?: throw BpdmNullMappingException(locality::class, LocalityDto::class, locality::value),
            locality.shortName,
            toTypeOrDefault(locality.type)
        )
    }

    fun toDto(thoroughfare: ThoroughfareSaas): ThoroughfareDto {
        return ThoroughfareDto(
            thoroughfare.value ?: throw BpdmNullMappingException(thoroughfare::class, ThoroughfareDto::class, thoroughfare::value),
            thoroughfare.name,
            thoroughfare.shortName,
            thoroughfare.number,
            thoroughfare.direction,
            toTypeOrDefault(thoroughfare.type)
        )
    }

    fun toDto(premise: PremiseSaas): PremiseDto {
        return PremiseDto(
            premise.value ?: throw BpdmNullMappingException(premise::class, PremiseDto::class, premise::value),
            premise.shortName,
            premise.number,
            toTypeOrDefault(premise.type)
        )
    }

    fun toDto(deliveryPoint: PostalDeliveryPointSaas): PostalDeliveryPointDto {
        return PostalDeliveryPointDto(
            deliveryPoint.value ?: throw BpdmNullMappingException(deliveryPoint::class, PostalDeliveryPointDto::class, deliveryPoint::value),
            deliveryPoint.shortName,
            deliveryPoint.number,
            toTypeOrDefault(deliveryPoint.type)
        )
    }

    fun toDto(geoCoords: GeoCoordinatesSaas): GeoCoordinateDto? {
        return if (geoCoords.latitude != null && geoCoords.longitude != null) GeoCoordinateDto(geoCoords.longitude, geoCoords.latitude, null) else null
    }

    fun toRelationToDelete(relation: RelationSaas): DeleteRelationsRequestSaas.RelationToDeleteSaas {
        return DeleteRelationsRequestSaas.RelationToDeleteSaas(
            startNode = DeleteRelationsRequestSaas.RelationNodeToDeleteSaas(
                dataSourceId = relation.startNodeDataSource,
                externalId = relation.startNode
            ),
            endNode = DeleteRelationsRequestSaas.RelationNodeToDeleteSaas(
                dataSourceId = relation.endNodeDataSource,
                externalId = relation.endNode
            )
        )
    }
}