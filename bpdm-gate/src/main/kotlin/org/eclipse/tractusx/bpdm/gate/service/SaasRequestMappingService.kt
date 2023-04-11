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
        val legalEntity = request.legalEntity
        return BusinessPartnerSaas(
            externalId = request.externalId,
            dataSource = saasConfigProperties.datasource,
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.LEGAL_ENTITY.name)),
            identifiers = toLegalEntityIdentifiersSaas(legalEntity.identifiers, request.bpn),
            names = toNamesSaas(legalEntity.legalName),
            status = legalEntity.states.map { it.toSaasModel() }.firstOrNull(),
            legalForm = toLegalFormSaas(legalEntity.legalForm),
            profile = toPartnerProfileSaas(legalEntity.classifications),
            // Known issue: name, state, BPN-A and identifiers of the legal address are not transferred to SaaS yet!!
            addresses = toAddressesSaasModel(legalEntity.legalAddress)
        )
    }

    fun toSaasModel(request: SiteGateInputRequest): BusinessPartnerSaas {
        val site = request.site
        return BusinessPartnerSaas(
            externalId = request.externalId,
            dataSource = saasConfigProperties.datasource,
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.ORGANIZATIONAL_UNIT.name)),
            identifiers = toIdentifiersSaas(request.bpn),
            names = toNamesSaas(site.name),
            status = site.states.map { it.toSaasModel() }.firstOrNull(),
            // Known issue: Name, state, BPN-A and identifiers of the main address are not transferred to SaaS yet!!
            addresses = toAddressesSaasModel(site.mainAddress)
        )
        // Parent relation is updated later in SiteService.upsertParentRelations()
    }

    fun toSaasModel(request: AddressGateInputRequest): BusinessPartnerSaas {
        val address = request.address
        return BusinessPartnerSaas(
            externalId = request.externalId,
            dataSource = saasConfigProperties.datasource,
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.BP_ADDRESS.name)),
            identifiers = toAddressIdentifiersSaas(address.identifiers, request.bpn),
            names = toNamesSaas(address.name),
            status = address.states.map { it.toSaasModel() }.firstOrNull(),
            addresses = toAddressesSaasModel(address)
        )
        // Parent relation is updated later in AddressService.upsertParentRelations()
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

    private fun AddressStateDto.toSaasModel(): BusinessPartnerStatusSaas {
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

    private fun AddressIdentifierDto.toSaasModel(): IdentifierSaas {
        return IdentifierSaas(
            type = TypeKeyNameUrlSaas(type),
            value = value
        )
    }

    private fun toAddressesSaasModel(address: LogisticAddressDto): Collection<AddressSaas> {
        val physicalAddress = address.physicalPostalAddress.let { toPhysicalAddressSaasModel(it) }
        val alternativeAddress = address.alternativePostalAddress?.let { toAlternativeAddressSaasModel(it) }
        return listOfNotNull(physicalAddress, alternativeAddress)
    }

    private fun toPhysicalAddressSaasModel(address: PhysicalPostalAddressDto): AddressSaas {
        val mapping = SaasDtoToSaasAddressMapping(address.baseAddress)
        return AddressSaas(
                 id = "0",
             externalId = null,
             saasId = null,
             version = null,
             identifyingName = null,
             careOf = null,
             contexts = emptyList(),
             country = mapping.country(),
             administrativeAreas = mapping.administrativeAreas(),
             postCodes = mapping.postcodes(),
             localities = mapping.localities(),
             thoroughfares = mapping.thoroughfares(address),
             premises = mapping.premises(address),
             postalDeliveryPoints =  emptyList(),
             geographicCoordinates = mapping.geoCoordinates(),
             types = emptyList(),
             metadataSaas = AddressMetadataSaas()
        )
    }

    private fun toAlternativeAddressSaasModel(address: AlternativePostalAddressDto): AddressSaas {
        val mapping = SaasDtoToSaasAddressMapping(address.baseAddress)
        return AddressSaas(
            id = "0",
            externalId = null,
            saasId = null,
            version = null,
            identifyingName = null,
            careOf = null,
            contexts = emptyList(),
            country = mapping.country(),
            administrativeAreas = mapping.administrativeAreas(),
            postCodes = mapping.postcodes(),
            localities = mapping.localities(),
            thoroughfares = mapping.thoroughfares(physicalAddress = null),
            premises = mapping.premises(physicalAddress = null),
            postalDeliveryPoints = mapping.postalDeliveryPoints(address),
            geographicCoordinates = mapping.geoCoordinates(),
            types = emptyList(),
            metadataSaas = AddressMetadataSaas()
        )
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

    private fun toNamesSaas(nameDto: NameDto): List<NameSaas> =
        listOf(nameDto.toSaasModel())

    private fun toNamesSaas(name: String?): List<NameSaas> =
        name?.let { listOf(NameSaas(value = it)) } ?: emptyList()

    private fun toLegalEntityIdentifiersSaas(identifiers: Collection<LegalEntityIdentifierDto>, bpn: String?): Collection<IdentifierSaas> {
        val identifiersSaas = identifiers.map { it.toSaasModel() }
        return when (bpn) {
            null -> identifiersSaas
            else -> identifiersSaas.plus(createBpnIdentifierSaas(bpn))
        }
    }

    private fun toAddressIdentifiersSaas(identifiers: Collection<AddressIdentifierDto>, bpn: String?): Collection<IdentifierSaas> {
        val identifiersSaas = identifiers.map { it.toSaasModel() }
        return when (bpn) {
            null -> identifiersSaas
            else -> identifiersSaas.plus(createBpnIdentifierSaas(bpn))
        }
    }

    private fun toIdentifiersSaas(bpn: String?): Collection<IdentifierSaas> =
        bpn?.let { listOf(createBpnIdentifierSaas(it)) } ?: emptyList()

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