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

import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.common.model.SaasAddressType
import org.eclipse.tractusx.bpdm.common.model.toSaasTypeDto
import org.eclipse.tractusx.bpdm.gate.api.model.LogisticAddressGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddressGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateInputRequest
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
            identifiers = toLegalEntityIdentifiersSaas(legalEntity.identifiers, ""),
            names = toNamesSaas(request.legalNameParts[0]),
            // TODO Only the first state is passed to SaaS, any others are ignored
            status = legalEntity.states.map { it.toSaasModel() }.firstOrNull(),
            legalForm = toLegalFormSaas(legalEntity.legalForm),
            profile = toPartnerProfileSaas(legalEntity.classifications),
            // TODO Known issue: name, state, BPN-A and identifiers of the legal address are not transferred to SaaS yet!!
            addresses = toAddressesSaasModel(request.legalAddress)
        )
    }

    fun toSaasModel(request: SiteGateInputRequest): BusinessPartnerSaas {
        val site = request.site
        return BusinessPartnerSaas(
            externalId = request.externalId,
            dataSource = saasConfigProperties.datasource,
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.ORGANIZATIONAL_UNIT.name)),
            identifiers = toIdentifiersSaas(""),
            names = toNamesSaas(site.nameParts.firstOrNull()),
            status = site.states.map { it.toSaasModel() }.firstOrNull(),
            // TODO Known issue: Name, state, BPN-A and identifiers of the main address are not transferred to SaaS yet!!
            addresses = toAddressesSaasModel(request.mainAddress)
        )
        // Parent relation is updated later in SiteService.upsertParentRelations()
    }

    fun toSaasModel(request: AddressGateInputRequest): BusinessPartnerSaas {
        val address = request.address
        return BusinessPartnerSaas(
            externalId = request.externalId,
            dataSource = saasConfigProperties.datasource,
            types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.BP_ADDRESS.name)),
            identifiers = toAddressIdentifiersSaas(address.identifiers, ""),
            names = toNamesSaas(address.nameParts.firstOrNull() ?: ""),
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

    private fun toAddressesSaasModel(address: LogisticAddressGateDto): Collection<AddressSaas> {
        val physicalAddress = address.physicalPostalAddress.let { toPhysicalAddressSaasModel(it) }
        val alternativeAddress = address.alternativePostalAddress?.let { toAlternativeAddressSaasModel(it) }
        return listOfNotNull(physicalAddress, alternativeAddress)
    }

    private fun toPhysicalAddressSaasModel(address: PhysicalPostalAddressGateDto): AddressSaas {
        val mapping = SaasDtoToSaasAddressMapping(address.baseAddress)
        return AddressSaas(
            country = mapping.country(),
            administrativeAreas = mapping.administrativeAreas(physicalAddress = address),
            postCodes = mapping.postcodes(physicalAddress = address),
            localities = mapping.localities(physicalAddress = address),
            thoroughfares = mapping.thoroughfares(physicalAddress = address),
            premises = mapping.premises(physicalAddress = address),
            postalDeliveryPoints = emptyList(),
            geographicCoordinates = mapping.geoCoordinates(),
            types = listOf(SaasAddressType.LEGAL.toSaasTypeDto())
        )
    }

    private fun toAlternativeAddressSaasModel(address: AlternativePostalAddressDto): AddressSaas {
        val mapping = SaasDtoToSaasAddressMapping(address.baseAddress)
        return AddressSaas(
            country = mapping.country(),
            administrativeAreas = mapping.administrativeAreas(alternateAddress = address),
            postCodes = mapping.postcodes(physicalAddress = null),
            localities = mapping.localities(alternateAddress = address),
            thoroughfares = mapping.thoroughfares(physicalAddress = null),
            premises = mapping.premises(physicalAddress = null),
            postalDeliveryPoints = mapping.postalDeliveryPoints(alternativeAddress = address),
            geographicCoordinates = mapping.geoCoordinates(),
            types = listOf(SaasAddressType.LEGAL_ALTERNATIVE.toSaasTypeDto())
        )
    }

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
}