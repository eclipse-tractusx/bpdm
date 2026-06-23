/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.test.testdata.gate.v7

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.SiteScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.StreetDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import java.time.Instant

class BusinessPartnerOutputDtoV7Factory {

    fun fromLegalEntity(input: BusinessPartnerInputDto, legalEntity: LegalEntityWithLegalAddressVerboseDto): BusinessPartnerOutputDto{
        return BusinessPartnerOutputDto(
            externalId = input.externalId,
            nameParts = input.nameParts,
            identifiers = legalEntity.header.identifiers.map { BusinessPartnerIdentifierDto(it.type, it.value, it.issuingBody) },
            states = legalEntity.header.states.map { BusinessPartnerStateDto(it.validFrom, it.validTo, it.type) },
            roles = input.roles,
            isOwnCompanyData = input.isOwnCompanyData,
            legalEntity = buildLegalEntityRepresentation(legalEntity.header),
            site = null,
            address = buildAddressRepresentation(legalEntity.legalAddress, AddressType.LegalAddress),
            externalSequenceTimestamp = null,
            scriptVariants = legalEntity.scriptVariants.map { buildLegalEntityScriptVariant(it.scriptCode, it) },
            createdAt = Instant.MIN,
            updatedAt = Instant.MIN
        )
    }

    fun fromLegalEntityOnSite(input: BusinessPartnerInputDto, legalEntity: LegalEntityWithLegalAddressVerboseDto, site: SiteWithMainAddressVerboseDto): BusinessPartnerOutputDto{
        return BusinessPartnerOutputDto(
            externalId = input.externalId,
            nameParts = input.nameParts,
            identifiers = legalEntity.header.identifiers.map { BusinessPartnerIdentifierDto(it.type, it.value, it.issuingBody) },
            states = legalEntity.header.states.map { BusinessPartnerStateDto(it.validFrom, it.validTo, it.type) },
            roles = input.roles,
            isOwnCompanyData = input.isOwnCompanyData,
            legalEntity = buildLegalEntityRepresentation(legalEntity.header),
            site = buildSiteRepresentation(site.site),
            address = buildAddressRepresentation(legalEntity.legalAddress, AddressType.LegalAndSiteMainAddress),
            externalSequenceTimestamp = null,
            scriptVariants = run {
                val leByCode = legalEntity.scriptVariants.associateBy { it.scriptCode }
                val siteByCode = site.site.scriptVariants.associateBy { it.scriptCode }
                (leByCode.keys + siteByCode.keys).map { code -> buildLegalEntityScriptVariant(code, leByCode[code], siteByCode[code]) }
            },
            createdAt = Instant.MIN,
            updatedAt = Instant.MIN
        )
    }

    fun fromSite(input: BusinessPartnerInputDto, legalEntity: LegalEntityWithLegalAddressVerboseDto, site: SiteWithMainAddressVerboseDto): BusinessPartnerOutputDto{
        return BusinessPartnerOutputDto(
            externalId = input.externalId,
            nameParts = input.nameParts,
            identifiers = emptyList(),
            states = site.site.states.map { BusinessPartnerStateDto(it.validFrom, it.validTo, it.type) },
            roles = input.roles,
            isOwnCompanyData = input.isOwnCompanyData,
            legalEntity = buildLegalEntityRepresentation(legalEntity.header),
            site = buildSiteRepresentation(site.site),
            address = buildAddressRepresentation(site.mainAddress, AddressType.SiteMainAddress),
            externalSequenceTimestamp = null,
            scriptVariants = run {
                val leByCode = legalEntity.scriptVariants.associateBy { it.scriptCode }
                val siteByCode = site.site.scriptVariants.associateBy { it.scriptCode }
                (leByCode.keys + siteByCode.keys).map { code -> buildSiteScriptVariant(code, leByCode[code], siteByCode[code]) }
            },
            createdAt = Instant.MIN,
            updatedAt = Instant.MIN
        )
    }

    fun fromAdditionalAddressOnSite(
        input: BusinessPartnerInputDto,
        legalEntity: LegalEntityWithLegalAddressVerboseDto,
        site: SiteWithMainAddressVerboseDto,
        additionalAddress: LogisticAddressVerboseDto
    ): BusinessPartnerOutputDto{
        return BusinessPartnerOutputDto(
            externalId = input.externalId,
            nameParts = input.nameParts,
            identifiers = additionalAddress.address.identifiers.map { BusinessPartnerIdentifierDto(it.type, it.value, null) },
            states = additionalAddress.address.states.map { BusinessPartnerStateDto(it.validFrom, it.validTo, it.type) },
            roles = input.roles,
            isOwnCompanyData = input.isOwnCompanyData,
            legalEntity = buildLegalEntityRepresentation(legalEntity.header),
            site = buildSiteRepresentation(site.site),
            address = buildAddressRepresentation(additionalAddress.address, AddressType.AdditionalAddress),
            externalSequenceTimestamp = null,
            scriptVariants = run {
                val leByCode = legalEntity.scriptVariants.associateBy { it.scriptCode }
                val siteByCode = site.site.scriptVariants.associateBy { it.scriptCode }
                val addByCode = additionalAddress.scriptVariants.associateBy { it.scriptCode }
                (leByCode.keys + siteByCode.keys + addByCode.keys).map { code -> buildAdditionalAddressScriptVariant(code, leByCode[code], addByCode[code], siteByCode[code]) }
            },
            createdAt = Instant.MIN,
            updatedAt = Instant.MIN
        )
    }

    private fun buildLegalEntityRepresentation(legalEntity: LegalEntityHeaderVerboseDto): LegalEntityRepresentationOutputDto{
        return LegalEntityRepresentationOutputDto(
            legalEntityBpn = legalEntity.bpnl,
            legalName = legalEntity.legalName,
            shortName = legalEntity.legalShortName,
            legalForm = legalEntity.legalForm,
            confidenceCriteria = buildConfidence(legalEntity.confidenceCriteria),
            states = legalEntity.states.map { BusinessPartnerStateDto(it.validFrom, it.validTo, it.type) }
        )
    }

    private fun buildAddressRepresentation(logisticAddress: LogisticAddressInvariantVerboseDto, addressType: AddressType): AddressComponentOutputDto{
        return AddressComponentOutputDto(
            addressBpn = logisticAddress.bpna,
            name = logisticAddress.name,
            addressType = addressType,
            physicalPostalAddress = buildPhysicalAddress(logisticAddress.physicalPostalAddress),
            alternativePostalAddress = logisticAddress.alternativePostalAddress?.let { buildAlternativeAddress(it) },
            confidenceCriteria = buildConfidence(logisticAddress.confidenceCriteria),
            states = logisticAddress.states.map { BusinessPartnerStateDto(it.validFrom, it.validTo, it.type) }
        )
    }

    private fun buildPhysicalAddress(physicalAddress: PhysicalPostalAddressVerboseDto): PhysicalPostalAddressDto{
        return PhysicalPostalAddressDto(
            geographicCoordinates = physicalAddress.geographicCoordinates,
            country = physicalAddress.country,
            administrativeAreaLevel1 = physicalAddress.administrativeAreaLevel1,
            administrativeAreaLevel2 = physicalAddress.administrativeAreaLevel2,
            administrativeAreaLevel3 = physicalAddress.administrativeAreaLevel3,
            postalCode = physicalAddress.postalCode,
            city = physicalAddress.city,
            district = physicalAddress.district,
            street = physicalAddress.street?.let { buildStreet(it) },
            companyPostalCode = physicalAddress.companyPostalCode,
            industrialZone = physicalAddress.industrialZone,
            building = physicalAddress.building,
            floor = physicalAddress.floor,
            door = physicalAddress.door,
            taxJurisdictionCode = physicalAddress.taxJurisdictionCode
        )
    }

    private fun buildAlternativeAddress(alternativeAddress: AlternativePostalAddressVerboseDto): AlternativePostalAddressDto{
        return AlternativePostalAddressDto(
            geographicCoordinates = alternativeAddress.geographicCoordinates,
            country = alternativeAddress.country,
            administrativeAreaLevel1 = alternativeAddress.administrativeAreaLevel1,
            postalCode = alternativeAddress.postalCode,
            city = alternativeAddress.city,
            deliveryServiceType = alternativeAddress.deliveryServiceType,
            deliveryServiceQualifier = alternativeAddress.deliveryServiceQualifier,
            deliveryServiceNumber = alternativeAddress.deliveryServiceNumber
        )
    }

    private fun buildStreet(street: org.eclipse.tractusx.bpdm.pool.api.model.StreetDto): StreetDto{
        return StreetDto(
            name = street.name,
            houseNumber = street.houseNumber,
            houseNumberSupplement = street.houseNumberSupplement,
            milestone = street.milestone,
            direction = street.direction,
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix
        )
    }

    private fun buildSiteRepresentation(site: SiteVerboseDto): SiteRepresentationOutputDto {
        return SiteRepresentationOutputDto(
            siteBpn = site.bpns,
            name = site.name,
            confidenceCriteria = buildConfidence(site.confidenceCriteria),
            states = site.states.map { BusinessPartnerStateDto(it.validFrom, it.validTo, it.type) }
        )
    }

    private fun buildConfidence(confidence: org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto): ConfidenceCriteriaDto{
        return with(confidence){
            ConfidenceCriteriaDto(
                sharedByOwner = sharedByOwner,
                checkedByExternalDataSource = checkedByExternalDataSource,
                numberOfSharingMembers = numberOfSharingMembers,
                lastConfidenceCheckAt = lastConfidenceCheckAt,
                nextConfidenceCheckAt = nextConfidenceCheckAt,
                confidenceLevel = confidenceLevel
            )
        }
    }

    private fun buildLegalEntityScriptVariant(scriptCode: String, legalEntityScriptVariant: LegalEntityScriptVariantDto?, site: org.eclipse.tractusx.bpdm.pool.api.model.SiteScriptVariantDto? = null): BusinessPartnerScriptVariantDto{
        return BusinessPartnerScriptVariantDto(
            scriptCode = scriptCode,
            nameParts = emptyList(),
            legalEntity = buildScriptVariantLegalEntityComponent(legalEntityScriptVariant),
            site = site?.let { buildScriptVariantSiteComponent(it) } ?: SiteScriptVariantDto(),
            address = buildScriptVariantAddressComponent(legalEntityScriptVariant?.legalAddress)
        )
    }

    private fun buildSiteScriptVariant(scriptCode: String, legalEntityScriptVariant: LegalEntityScriptVariantDto?, siteScriptVariant: org.eclipse.tractusx.bpdm.pool.api.model.SiteScriptVariantDto?): BusinessPartnerScriptVariantDto{
        return BusinessPartnerScriptVariantDto(
            scriptCode = scriptCode,
            nameParts = emptyList(),
            legalEntity = buildScriptVariantLegalEntityComponent(legalEntityScriptVariant),
            site = siteScriptVariant?.let { buildScriptVariantSiteComponent(it) } ?: SiteScriptVariantDto(),
            address = buildScriptVariantAddressComponent(siteScriptVariant?.mainAddress)
        )
    }

    private fun buildAdditionalAddressScriptVariant(scriptCode: String, legalEntityScriptVariant: LegalEntityScriptVariantDto?, addressScriptVariant: LogisticAddressScriptVariantDto?, site: org.eclipse.tractusx.bpdm.pool.api.model.SiteScriptVariantDto? = null): BusinessPartnerScriptVariantDto{
        return BusinessPartnerScriptVariantDto(
            scriptCode = scriptCode,
            nameParts = emptyList(),
            legalEntity = buildScriptVariantLegalEntityComponent(legalEntityScriptVariant),
            site = site?.let { buildScriptVariantSiteComponent(it) } ?: SiteScriptVariantDto(),
            address = buildScriptVariantAddressComponent(addressScriptVariant?.address)
        )
    }

    private fun buildScriptVariantLegalEntityComponent(legalEntityScriptVariant: LegalEntityScriptVariantDto?): org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityScriptVariantDto{
        return org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityScriptVariantDto(
            legalName = legalEntityScriptVariant?.legalName,
            shortName = legalEntityScriptVariant?.shortName
        )
    }

    private fun buildScriptVariantSiteComponent(siteScriptVariant: org.eclipse.tractusx.bpdm.pool.api.model.SiteScriptVariantDto?): SiteScriptVariantDto{
        return SiteScriptVariantDto(siteScriptVariant?.name)
    }

    private fun buildScriptVariantAddressComponent(addressScriptVariant: PostalAddressScriptVariantDto?): AddressScriptVariantDto {
        return AddressScriptVariantDto(
            name = addressScriptVariant?.addressName,
            physicalAddress = addressScriptVariant?.physicalAddress?.let { p ->
                PhysicalAddressScriptVariantDto(
                    postalCode = p.postalCode,
                    city = p.city,
                    district = p.district,
                    street = p.street?.let { buildStreet(it) } ?: StreetDto(),
                    companyPostalCode = p.companyPostalCode,
                    industrialZone = p.industrialZone,
                    building = p.building,
                    floor = p.floor,
                    door = p.door,
                    taxJurisdictionCode = p.taxJurisdictionCode
                )
            } ?: PhysicalAddressScriptVariantDto(null, null, null, StreetDto(), null, null, null, null, null, null),
            alternativeAddress = addressScriptVariant?.alternativeAddress?.let { a ->
                org.eclipse.tractusx.bpdm.gate.api.model.AlternativeAddressScriptVariantDto(
                    postalCode = a.postalCode,
                    city = a.city,
                    deliveryServiceQualifier = a.deliveryServiceQualifier,
                    deliveryServiceNumber = a.deliveryServiceNumber
                )
            }
        )
    }

}