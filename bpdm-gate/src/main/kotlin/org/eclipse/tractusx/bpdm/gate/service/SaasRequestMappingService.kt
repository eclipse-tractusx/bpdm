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
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.config.SaasConfigProperties
import org.springframework.stereotype.Service

@Service
class SaasRequestMappingService(
    private val bpnConfigProperties: BpnConfigProperties,
    private val saasConfigProperties: SaasConfigProperties
) {
    fun toSaasModel(request: LegalEntityGateInputRequest): BusinessPartnerSaas {
        return toSaasModel(request.legalEntity, request.externalId, request.bpn)
    }

    fun toSaasModel(request: SiteGateInputRequest): BusinessPartnerSaas {
        return BusinessPartnerSaas(
            externalId = request.externalId,
            dataSource = saasConfigProperties.datasource,
            names = listOf(NameSaas(value = request.site.name)),
            status = request.site.states.map { it.toSaasModel() }.firstOrNull(),
            addresses = listOf(toSaasModel(request.site.mainAddress)),
            identifiers = request.bpn?.let { listOf(createBpnIdentifierSaas(it)) } ?: emptyList(),
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.ORGANIZATIONAL_UNIT.name))
        )
    }

    fun toSaasModel(request: AddressGateInputRequest): BusinessPartnerSaas {
        return BusinessPartnerSaas(
            externalId = request.externalId,
            dataSource = saasConfigProperties.datasource,
            addresses = listOf(toSaasModel(request.address)),
            identifiers = request.bpn?.let { listOf(createBpnIdentifierSaas(it)) } ?: emptyList(),
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.BP_ADDRESS.name))
        )
    }

    private fun toSaasModel(legalEntity: LegalEntityDto, externalId: String, bpn: String?): BusinessPartnerSaas {
        return BusinessPartnerSaas(
            externalId = externalId,
            dataSource = saasConfigProperties.datasource,
            identifiers = toIdentifiersSaas(legalEntity.identifiers, bpn),
            names = listOf(legalEntity.legalName.toSaasModel()),
            legalForm = toLegalFormSaas(legalEntity.legalForm),
            status = legalEntity.states.map { it.toSaasModel() }.firstOrNull(),
            profile = toPartnerProfileSaas(legalEntity.classifications),
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.LEGAL_ENTITY.name)),
            addresses = listOf(toSaasModel(legalEntity.legalAddress))
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
            type = TypeKeyNameUrlSaas(type.name)
        )
    }

    private fun LegalEntityStateDto.toSaasModel(): BusinessPartnerStatusSaas {
        return BusinessPartnerStatusSaas(
            type = TypeKeyNameUrlSaas(type.name),
            officialDenotation = officialDenotation,
            validFrom = validFrom,
            validUntil = validTo
        )
    }

    private fun SiteStateDto.toSaasModel(): BusinessPartnerStatusSaas {
        return BusinessPartnerStatusSaas(
            type = TypeKeyNameUrlSaas(type.name),
            officialDenotation = description,
            validFrom = validFrom,
            validUntil = validTo
        )
    }

    private fun toLegalFormSaas(technicalKey: String?) = if (technicalKey != null) LegalFormSaas(technicalKey = technicalKey) else null

    private fun NameDto.toSaasModel(): NameSaas {
        return NameSaas(
            value = value,
            shortName = shortName
        )
    }

    private fun LegalEntityIdentifierDto.toSaasModel(): IdentifierSaas {
        return IdentifierSaas(
            type = TypeKeyNameUrlSaas(type),
            value = value,
            issuingBody = TypeKeyNameUrlSaas(name = issuingBody)
        )
    }

    // TODO still unused!
    private fun AddressIdentifierDto.toSaasModel(): IdentifierSaas {
        return IdentifierSaas(
            type = TypeKeyNameUrlSaas(type),
            value = value
        )
    }

    private fun toSaasModel(address: LogisticAddressDto): AddressSaas {
        return with(address) {
            AddressSaas(
                // TODO Mapping
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


    private fun toSaasModel(geoCoordinate: GeoCoordinateDto?): GeoCoordinatesSaas? =
        geoCoordinate?.let { GeoCoordinatesSaas(it.longitude, it.latitude) }

    private fun toIdentifiersSaas(identifiers: Collection<LegalEntityIdentifierDto>, bpn: String?): Collection<IdentifierSaas> {
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
            issuingBody = TypeKeyNameUrlSaas(name = bpnConfigProperties.agencyName)
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