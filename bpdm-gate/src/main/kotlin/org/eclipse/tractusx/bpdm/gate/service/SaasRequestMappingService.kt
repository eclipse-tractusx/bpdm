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
    fun toCdqModel(legalEntity: LegalEntityGateInput): BusinessPartnerSaas {
        return toCdqModel(legalEntity.legalEntity, legalEntity.externalId, legalEntity.bpn)
    }

    fun toCdqModel(site: SiteGateInput): BusinessPartnerSaas {
        return BusinessPartnerSaas(
            externalId = site.externalId,
            dataSource = saasConfigProperties.datasource,
            names = listOf(NameSaas(value = site.site.name)),
            addresses = listOf(toCdqModel(site.site.mainAddress)),
            identifiers = if (site.bpn != null) listOf(createBpnIdentifierCdq(site.bpn)) else emptyList(),
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.ORGANIZATIONAL_UNIT.name))
        )
    }

    fun toCdqModel(address: AddressGateInput): BusinessPartnerSaas {
        return BusinessPartnerSaas(
            externalId = address.externalId,
            dataSource = saasConfigProperties.datasource,
            addresses = listOf(toCdqModel(address.address)),
            identifiers = if (address.bpn != null) listOf(createBpnIdentifierCdq(address.bpn)) else emptyList(),
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.BP_ADDRESS.name))
        )
    }

    private fun toCdqModel(legalEntity: LegalEntityDto, externalId: String, bpn: String?): BusinessPartnerSaas {
        return BusinessPartnerSaas(
            externalId = externalId,
            dataSource = saasConfigProperties.datasource,
            identifiers = toIdentifiersCdq(legalEntity.identifiers, bpn),
            names = legalEntity.names.map { it.toCdqModel() },
            legalForm = toLegalFormCdq(legalEntity.legalForm),
            status = legalEntity.status?.toCdqModel(),
            profile = toPartnerProfileCdq(legalEntity.profileClassifications),
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.LEGAL_ENTITY.name)),
            bankAccounts = legalEntity.bankAccounts.map { it.toCdqModel() },
            addresses = listOf(toCdqModel(legalEntity.legalAddress))
        )
    }

    private fun BankAccountDto.toCdqModel(): BankAccountSaas {
        return BankAccountSaas(
            internationalBankAccountIdentifier = internationalBankAccountIdentifier,
            internationalBankIdentifier = internationalBankIdentifier,
            nationalBankAccountIdentifier = nationalBankAccountIdentifier,
            nationalBankIdentifier = nationalBankIdentifier
        )
    }

    private fun toPartnerProfileCdq(profileClassifications: Collection<ClassificationDto>): PartnerProfileSaas? {
        if (profileClassifications.isEmpty()) {
            return null
        }
        return PartnerProfileSaas(classifications = profileClassifications.map { it.toCdqModel() })
    }

    private fun ClassificationDto.toCdqModel(): ClassificationSaas {
        return ClassificationSaas(
            value = value,
            code = code,
            type = if (type != null) TypeKeyNameUrlSaas(type!!.name) else null
        )
    }

    private fun BusinessStatusDto.toCdqModel(): BusinessPartnerStatusSaas {
        return BusinessPartnerStatusSaas(
            type = TypeKeyNameUrlSaas(type.name),
            officialDenotation = officialDenotation,
            validFrom = validFrom,
            validUntil = validUntil
        )
    }

    private fun toLegalFormCdq(technicalKey: String?) = if (technicalKey != null) LegalFormSaas(technicalKey = technicalKey) else null

    private fun NameDto.toCdqModel(): NameSaas {
        return NameSaas(
            value = value,
            shortName = shortName,
            type = TypeKeyNameUrlSaas(type.name),
            language = toLanguageCdq(this.language)
        )
    }

    private fun IdentifierDto.toCdqModel(): IdentifierSaas {
        return IdentifierSaas(
            type = TypeKeyNameUrlSaas(type),
            value = value,
            issuingBody = TypeKeyNameUrlSaas(issuingBody),
            status = TypeKeyNameSaas(status)
        )
    }

    private fun toCdqModel(address: AddressDto): AddressSaas {
        return with(address) {
            AddressSaas(
                version = toCdqModel(version),
                careOf = toCareOfCdq(careOf),
                contexts = contexts.map { toContextCdq(it) },
                country = toCountryCdq(country),
                administrativeAreas = administrativeAreas.map { toCdqModel(it, version.language) },
                postCodes = postCodes.map { toCdqModel(it) },
                localities = localities.map { toCdqModel(it, version.language) },
                thoroughfares = thoroughfares.map { toCdqModel(it, version.language) },
                postalDeliveryPoints = postalDeliveryPoints.map { toCdqModel(it, version.language) },
                premises = premises.map { toCdqModel(it, version.language) },
                geographicCoordinates = toCdqModel(geographicCoordinates),
                types = types.map { toKeyNameUrlTypeCdq(it) }
            )
        }
    }

    private fun toCdqModel(version: AddressVersionDto): AddressVersionSaas? {
        val languageCdq = toLanguageCdq(version.language)
        val characterSetCdq = toCharacterSetCdq(version.characterSet)

        return if (languageCdq == null && characterSetCdq == null) null else AddressVersionSaas(languageCdq, characterSetCdq)
    }


    private fun toCareOfCdq(careOf: String?): WrappedValueSaas? =
        if (careOf != null) WrappedValueSaas(careOf) else null

    private fun toContextCdq(context: String): WrappedValueSaas =
        WrappedValueSaas(context)

    private fun toCdqModel(adminArea: AdministrativeAreaDto, languageCode: LanguageCode): AdministrativeAreaSaas =
        AdministrativeAreaSaas(adminArea.value, adminArea.shortName, toKeyNameUrlTypeCdq(adminArea.type), toLanguageCdq(languageCode))


    private fun toCdqModel(postcode: PostCodeDto): PostCodeSaas =
        PostCodeSaas(postcode.value, toKeyNameUrlTypeCdq(postcode.type))

    private fun toCdqModel(locality: LocalityDto, languageCode: LanguageCode): LocalitySaas =
        LocalitySaas(toKeyNameUrlTypeCdq(locality.type), locality.shortName, locality.value, toLanguageCdq(languageCode))

    private fun toCdqModel(thoroughfare: ThoroughfareDto, languageCode: LanguageCode): ThoroughfareSaas =
        ThoroughfareSaas(
            toKeyNameUrlTypeCdq(thoroughfare.type),
            thoroughfare.shortName,
            thoroughfare.number,
            thoroughfare.value,
            thoroughfare.name,
            thoroughfare.direction,
            toLanguageCdq(languageCode)
        )

    private fun toCdqModel(deliveryPoint: PostalDeliveryPointDto, languageCode: LanguageCode): PostalDeliveryPointSaas =
        PostalDeliveryPointSaas(
            toKeyNameUrlTypeCdq(deliveryPoint.type),
            deliveryPoint.shortName,
            deliveryPoint.number,
            deliveryPoint.value,
            toLanguageCdq(languageCode)
        )

    private fun toCdqModel(premise: PremiseDto, languageCode: LanguageCode): PremiseSaas =
        PremiseSaas(toKeyNameUrlTypeCdq(premise.type), premise.shortName, premise.number, premise.value, toLanguageCdq(languageCode))


    private fun toCdqModel(geoCoordinate: GeoCoordinateDto?): GeoCoordinatesSaas? =
        geoCoordinate?.let { GeoCoordinatesSaas(it.longitude, it.latitude) }

    private fun toIdentifiersCdq(identifiers: Collection<IdentifierDto>, bpn: String?): Collection<IdentifierSaas> {
        var identifiersCdq = identifiers.map { it.toCdqModel() }
        if (bpn != null) {
            identifiersCdq = identifiersCdq.plus(createBpnIdentifierCdq(bpn))
        }
        return identifiersCdq
    }

    private fun createBpnIdentifierCdq(bpn: String): IdentifierSaas {
        return IdentifierSaas(
            type = TypeKeyNameUrlSaas(bpnConfigProperties.id, bpnConfigProperties.name),
            value = bpn,
            issuingBody = TypeKeyNameUrlSaas(bpnConfigProperties.agencyKey, bpnConfigProperties.agencyName)
        )
    }

    private inline fun <reified T> toKeyNameTypeCdq(type: Enum<T>): TypeKeyNameSaas where T : Enum<T> =
        TypeKeyNameSaas(type.name, null)

    private inline fun <reified T> toKeyNameUrlTypeCdq(type: Enum<T>): TypeKeyNameUrlSaas where T : Enum<T> =
        TypeKeyNameUrlSaas(type.name, null)

    private fun toLanguageCdq(technicalKey: LanguageCode) =
        if (technicalKey != LanguageCode.undefined) LanguageSaas(technicalKey, null) else null

    private fun toCountryCdq(countryCode: CountryCode) =
        if (countryCode != CountryCode.UNDEFINED) CountrySaas(countryCode, null) else null

    private fun toCharacterSetCdq(characterSet: CharacterSet) =
        if (characterSet != CharacterSet.UNDEFINED) toKeyNameTypeCdq(characterSet) else null
}