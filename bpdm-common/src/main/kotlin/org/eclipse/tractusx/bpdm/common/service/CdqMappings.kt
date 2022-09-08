/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.HasDefaultValue

object CdqMappings {

    fun findBpn(identifiers: Collection<IdentifierCdq>): String? {
        return identifiers.find { it.type?.technicalKey == "CX_BPN" }?.value
    }

    private fun toReference(type: TypeKeyNameUrlCdq?): String {
        return type!!.technicalKey!!
    }

    private fun toOptionalReference(type: TypeKeyNameUrlCdq?): String? {
        return type?.technicalKey
    }

    private fun toOptionalReference(type: TypeKeyNameCdq?): String? {
        return type?.technicalKey
    }

    fun toOptionalReference(legalForm: LegalFormCdq?): String? {
        return legalForm?.technicalKey
    }

    private inline fun <reified T> toType(type: TypeKeyNameUrlCdq): T where T : Enum<T> {
        return enumValueOf(type.technicalKey!!)
    }

    inline fun <reified T> toTypeOrDefault(type: TypeKeyNameUrlCdq?): T where T : Enum<T>, T : HasDefaultValue<T> {
        return technicalKeyToType(type?.technicalKey)
    }

    inline fun <reified T> technicalKeyToType(technicalKey: String?): T where T : Enum<T>, T : HasDefaultValue<T> {
        val allValues = enumValues<T>()
        val foundValue = if (technicalKey != null) allValues.map { it.name }.find { technicalKey == it } else null
        return if (foundValue != null) enumValueOf(foundValue) else allValues.first().getDefault()
    }

    fun toLanguageCode(language: LanguageCdq?): LanguageCode {
        return language?.technicalKey ?: LanguageCode.undefined
    }

    inline fun <reified T> toTypeOrDefault(type: TypeKeyNameCdq?): T where T : Enum<T>, T : HasDefaultValue<T> {
        return technicalKeyToType(type?.technicalKey)
    }

    fun toCountryCode(country: CountryCdq?): CountryCode {
        return country?.shortName ?: CountryCode.UNDEFINED
    }

    fun BusinessPartnerCdq.toLegalEntityDto(): LegalEntityDto {
        return LegalEntityDto(
            identifiers = identifiers.filter { it.type?.technicalKey != "BPN" }.map { toDto(it) },
            names = names.map { toDto(it) },
            legalForm = toOptionalReference(legalForm),
            status = if (status != null) toDto(status) else null,
            profileClassifications = toDto(profile),
            types = types.map { toTypeOrDefault<BusinessPartnerType>(it) }.toSet(),
            bankAccounts = bankAccounts.map { toDto(it) },
            legalAddress = toDto(addresses.single())
        )
    }

    fun BusinessPartnerCdq.toSiteDto(): SiteDto {
        return SiteDto(
            name = names.single().value,
            mainAddress = toDto(addresses.single())
        )
    }

    fun toDto(identifier: IdentifierCdq): IdentifierDto {
        return IdentifierDto(
            identifier.value,
            toReference(identifier.type),
            toOptionalReference(identifier.issuingBody),
            toOptionalReference(identifier.status)
        )
    }

    fun toDto(name: NameCdq): NameDto {
        return NameDto(
            name.value,
            name.shortName,
            toTypeOrDefault(name.type),
            toLanguageCode(name.language)
        )
    }

    fun toDto(status: BusinessPartnerStatusCdq): BusinessStatusDto {
        return BusinessStatusDto(
            status.officialDenotation,
            status.validFrom,
            status.validUntil,
            toTypeOrDefault(status.type)
        )
    }

    fun toDto(profile: PartnerProfileCdq?): Collection<ClassificationDto> {
        return profile?.classifications?.map { toDto(it) } ?: emptyList()
    }

    fun toDto(classification: ClassificationCdq): ClassificationDto {
        return ClassificationDto(classification.value, classification.code, toType<ClassificationType>(classification.type!!))
    }

    fun toDto(account: BankAccountCdq): BankAccountDto {
        return BankAccountDto(
            emptyList(),
            CurrencyCode.UNDEFINED,
            account.internationalBankAccountIdentifier,
            account.internationalBankIdentifier,
            account.nationalBankAccountIdentifier,
            account.nationalBankIdentifier
        )
    }

    fun toDto(address: AddressCdq): AddressDto {
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

    fun toDto(version: AddressVersionCdq?): AddressVersionDto {
        return AddressVersionDto(toTypeOrDefault(version?.characterSet), toLanguageCode(version?.language))
    }

    fun toDto(area: AdministrativeAreaCdq): AdministrativeAreaDto {
        return AdministrativeAreaDto(
            area.value,
            area.shortName,
            null,
            toTypeOrDefault(area.type)
        )
    }

    fun toDto(postcode: PostCodeCdq): PostCodeDto {
        return PostCodeDto(postcode.value, toTypeOrDefault(postcode.type))
    }

    fun toDto(locality: LocalityCdq): LocalityDto {
        return LocalityDto(locality.value, locality.shortName, toTypeOrDefault(locality.type))
    }

    fun toDto(thoroughfare: ThoroughfareCdq): ThoroughfareDto {
        return ThoroughfareDto(
            thoroughfare.value,
            thoroughfare.name,
            thoroughfare.shortName,
            thoroughfare.number,
            thoroughfare.direction,
            toTypeOrDefault(thoroughfare.type)
        )
    }

    fun toDto(premise: PremiseCdq): PremiseDto {
        return PremiseDto(
            premise.value,
            premise.shortName,
            premise.number,
            toTypeOrDefault(premise.type)
        )
    }

    fun toDto(deliveryPoint: PostalDeliveryPointCdq): PostalDeliveryPointDto {
        return PostalDeliveryPointDto(
            deliveryPoint.value,
            deliveryPoint.shortName,
            deliveryPoint.number,
            toTypeOrDefault(deliveryPoint.type)
        )
    }

    fun toDto(geoCoords: GeoCoordinatesCdq): GeoCoordinateDto? {
        return if (geoCoords.latitude != null && geoCoords.longitude != null) GeoCoordinateDto(geoCoords.longitude, geoCoords.latitude, null) else null
    }

    fun toRelationToDelete(relation: RelationCdq): DeleteRelationsRequestCdq.RelationToDeleteCdq {
        return DeleteRelationsRequestCdq.RelationToDeleteCdq(
            startNode = DeleteRelationsRequestCdq.RelationNodeToDeleteCdq(
                dataSourceId = relation.startNodeDataSource,
                externalId = relation.startNode
            ),
            endNode = DeleteRelationsRequestCdq.RelationNodeToDeleteCdq(
                dataSourceId = relation.endNodeDataSource,
                externalId = relation.endNode
            )
        )
    }
}