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

package org.eclipse.tractusx.bpdm.gate.service

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.common.model.CharacterSet
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.config.SaasConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateInput
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInput
import org.springframework.stereotype.Service

@Service
class SaasRequestMappingService(
    private val bpnConfigProperties: BpnConfigProperties,
    private val saasConfigProperties: SaasConfigProperties
) {
    fun toSaasModel(legalEntity: LegalEntityGateInput): BusinessPartnerSaas {
        return toSaasModel(legalEntity.legalEntity, legalEntity.externalId, legalEntity.bpn)
    }

    fun toSaasModel(site: SiteGateInput): BusinessPartnerSaas {
        return BusinessPartnerSaas(
            externalId = site.externalId,
            dataSource = saasConfigProperties.datasource,
            names = listOf(NameSaas(value = site.site.name)),
            addresses = listOf(toSaasModel(site.site.mainAddress)),
            identifiers = if (site.bpn != null) listOf(createBpnIdentifierSaas(site.bpn)) else emptyList(),
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.ORGANIZATIONAL_UNIT.name))
        )
    }

    fun toSaasModel(address: AddressGateInput): BusinessPartnerSaas {
        return BusinessPartnerSaas(
            externalId = address.externalId,
            dataSource = saasConfigProperties.datasource,
            addresses = listOf(toSaasModel(address.address)),
            identifiers = if (address.bpn != null) listOf(createBpnIdentifierSaas(address.bpn)) else emptyList(),
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.BP_ADDRESS.name))
        )
    }

    private fun toSaasModel(legalEntity: LegalEntityDto, externalId: String, bpn: String?): BusinessPartnerSaas {
        return BusinessPartnerSaas(
            externalId = externalId,
            dataSource = saasConfigProperties.datasource,
            identifiers = toIdentifiersSaas(legalEntity.identifiers, bpn),
            names = legalEntity.names.map { it.toSaasModel() },
            legalForm = toLegalFormSaas(legalEntity.legalForm),
            status = legalEntity.status?.toSaasModel(),
            profile = toPartnerProfileSaas(legalEntity.profileClassifications),
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.LEGAL_ENTITY.name)),
            bankAccounts = legalEntity.bankAccounts.map { it.toSaasModel() },
            addresses = listOf(toSaasModel(legalEntity.legalAddress))
        )
    }

    private fun BankAccountDto.toSaasModel(): BankAccountSaas {
        return BankAccountSaas(
            internationalBankAccountIdentifier = internationalBankAccountIdentifier,
            internationalBankIdentifier = internationalBankIdentifier,
            nationalBankAccountIdentifier = nationalBankAccountIdentifier,
            nationalBankIdentifier = nationalBankIdentifier
        )
    }

    private fun toPartnerProfileSaas(profileClassifications: Collection<ClassificationDto>): PartnerProfileSaas? {
        if (profileClassifications.isEmpty()) {
            return null
        }
        return PartnerProfileSaas(classifications = profileClassifications.map { it.toSaasModel() })
    }

    private fun ClassificationDto.toSaasModel(): ClassificationSaas {
        return ClassificationSaas(
            value = value,
            code = code,
            type = if (type != null) TypeKeyNameUrlSaas(type!!.name) else null
        )
    }

    private fun BusinessStatusDto.toSaasModel(): BusinessPartnerStatusSaas {
        return BusinessPartnerStatusSaas(
            type = TypeKeyNameUrlSaas(type.name),
            officialDenotation = officialDenotation,
            validFrom = validFrom,
            validUntil = validUntil
        )
    }

    private fun toLegalFormSaas(technicalKey: String?) = if (technicalKey != null) LegalFormSaas(technicalKey = technicalKey) else null

    private fun NameDto.toSaasModel(): NameSaas {
        return NameSaas(
            value = value,
            shortName = shortName,
            type = TypeKeyNameUrlSaas(type.name),
            language = toLanguageSaas(this.language)
        )
    }

    private fun IdentifierDto.toSaasModel(): IdentifierSaas {
        return IdentifierSaas(
            type = TypeKeyNameUrlSaas(type),
            value = value,
            issuingBody = TypeKeyNameUrlSaas(issuingBody),
            status = TypeKeyNameSaas(status)
        )
    }

    private fun toSaasModel(address: AddressDto): AddressSaas {
        return with(address) {
            AddressSaas(
                version = toSaasModel(version),
                careOf = toCareOfSaas(careOf),
                contexts = contexts.map { toContextSaas(it) },
                country = toCountrySaas(country),
                administrativeAreas = administrativeAreas.map { toSaasModel(it, version.language) },
                postCodes = postCodes.map { toSaasModel(it) },
                localities = localities.map { toSaasModel(it, version.language) },
                thoroughfares = thoroughfares.map { toSaasModel(it, version.language) },
                postalDeliveryPoints = postalDeliveryPoints.map { toSaasModel(it, version.language) },
                premises = premises.map { toSaasModel(it, version.language) },
                geographicCoordinates = toSaasModel(geographicCoordinates),
                types = types.map { toKeyNameUrlTypeSaas(it) }
            )
        }
    }

    private fun toSaasModel(version: AddressVersionDto): AddressVersionSaas? {
        val languageSaas = toLanguageSaas(version.language)
        val characterSetSaas = toCharacterSetSaas(version.characterSet)

        return if (languageSaas == null && characterSetSaas == null) null else AddressVersionSaas(languageSaas, characterSetSaas)
    }


    private fun toCareOfSaas(careOf: String?): WrappedValueSaas? =
        if (careOf != null) WrappedValueSaas(careOf) else null

    private fun toContextSaas(context: String): WrappedValueSaas =
        WrappedValueSaas(context)

    private fun toSaasModel(adminArea: AdministrativeAreaDto, languageCode: LanguageCode): AdministrativeAreaSaas =
        AdministrativeAreaSaas(adminArea.value, adminArea.shortName, toKeyNameUrlTypeSaas(adminArea.type), toLanguageSaas(languageCode))


    private fun toSaasModel(postcode: PostCodeDto): PostCodeSaas =
        PostCodeSaas(postcode.value, toKeyNameUrlTypeSaas(postcode.type))

    private fun toSaasModel(locality: LocalityDto, languageCode: LanguageCode): LocalitySaas =
        LocalitySaas(toKeyNameUrlTypeSaas(locality.type), locality.shortName, locality.value, toLanguageSaas(languageCode))

    private fun toSaasModel(thoroughfare: ThoroughfareDto, languageCode: LanguageCode): ThoroughfareSaas =
        ThoroughfareSaas(
            toKeyNameUrlTypeSaas(thoroughfare.type),
            thoroughfare.shortName,
            thoroughfare.number,
            thoroughfare.value,
            thoroughfare.name,
            thoroughfare.direction,
            toLanguageSaas(languageCode)
        )

    private fun toSaasModel(deliveryPoint: PostalDeliveryPointDto, languageCode: LanguageCode): PostalDeliveryPointSaas =
        PostalDeliveryPointSaas(
            toKeyNameUrlTypeSaas(deliveryPoint.type),
            deliveryPoint.shortName,
            deliveryPoint.number,
            deliveryPoint.value,
            toLanguageSaas(languageCode)
        )

    private fun toSaasModel(premise: PremiseDto, languageCode: LanguageCode): PremiseSaas =
        PremiseSaas(toKeyNameUrlTypeSaas(premise.type), premise.shortName, premise.number, premise.value, toLanguageSaas(languageCode))


    private fun toSaasModel(geoCoordinate: GeoCoordinateDto?): GeoCoordinatesSaas? =
        geoCoordinate?.let { GeoCoordinatesSaas(it.longitude, it.latitude) }

    private fun toIdentifiersSaas(identifiers: Collection<IdentifierDto>, bpn: String?): Collection<IdentifierSaas> {
        var identifiersSaas = identifiers.map { it.toSaasModel() }
        if (bpn != null) {
            identifiersSaas = identifiersSaas.plus(createBpnIdentifierSaas(bpn))
        }
        return identifiersSaas
    }

    private fun createBpnIdentifierSaas(bpn: String): IdentifierSaas {
        return IdentifierSaas(
            type = TypeKeyNameUrlSaas(bpnConfigProperties.id, bpnConfigProperties.name),
            value = bpn,
            issuingBody = TypeKeyNameUrlSaas(bpnConfigProperties.agencyKey, bpnConfigProperties.agencyName)
        )
    }

    private inline fun <reified T> toKeyNameTypeSaas(type: Enum<T>): TypeKeyNameSaas where T : Enum<T> =
        TypeKeyNameSaas(type.name, null)

    private inline fun <reified T> toKeyNameUrlTypeSaas(type: Enum<T>): TypeKeyNameUrlSaas where T : Enum<T> =
        TypeKeyNameUrlSaas(type.name, null)

    private fun toLanguageSaas(technicalKey: LanguageCode) =
        if (technicalKey != LanguageCode.undefined) LanguageSaas(technicalKey, null) else null

    private fun toCountrySaas(countryCode: CountryCode) =
        if (countryCode != CountryCode.UNDEFINED) CountrySaas(countryCode, null) else null

    private fun toCharacterSetSaas(characterSet: CharacterSet) =
        if (characterSet != CharacterSet.UNDEFINED) toKeyNameTypeSaas(characterSet) else null
}