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

package org.eclipse.tractusx.bpdm.test.testdata.orchestrator

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressScriptVariantV7Dto as OrchestratorAlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto as OrchestratorConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.GeoCoordinateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.IdentifierV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityScriptVariantV7Dto as OrchestratorLegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressScriptVariantV7Dto as OrchestratorPhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantV7Dto as OrchestratorPostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantWithScriptCodeV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressWithScriptVariantsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteScriptVariantV7Dto as OrchestratorSiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetV7Dto as OrchestratorStreetV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetScriptVariantV7Dto as OrchestratorStreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedPropertiesV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedScriptVariantV7Dto
import java.time.LocalDateTime
import java.time.ZoneOffset

class RefinementTestDataFactory {

    fun buildLegalEntityBusinessPartner(
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseV7Dto,
        owningCompany: String?,
        nameParts: List<String>,
        scriptVariants: List<UncategorizedScriptVariantV7Dto> = emptyList()
    ): BusinessPartnerV7Dto{
        return BusinessPartnerV7Dto(
            nameParts = listOfNotNull(
                NamePartV7Dto(legalEntityGoldenRecord.header.legalName, NamePartTypeV7.LegalName),
                legalEntityGoldenRecord.header.legalShortName?.let { NamePartV7Dto(it, NamePartTypeV7.ShortName) },
                legalEntityGoldenRecord.header.legalForm?.let { NamePartV7Dto(it, NamePartTypeV7.LegalForm) }
            ),
            owningCompany = owningCompany,
            uncategorized = UncategorizedPropertiesV7Dto.empty.copy(nameParts = nameParts, scriptVariants = scriptVariants),
            legalEntity = buildLegalEntityComponent(legalEntityGoldenRecord),
            site = null,
            additionalAddress = null
        )
    }


    fun buildLegalEntityOnSiteBusinessPartner(
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseV7Dto,
        siteGoldenRecord: SiteVerboseV7Dto,
        owningCompany: String?,
        nameParts: List<String>,
        scriptVariants: List<UncategorizedScriptVariantV7Dto> = emptyList()
    ): BusinessPartnerV7Dto{
        return BusinessPartnerV7Dto(
            nameParts = listOfNotNull(
                NamePartV7Dto(legalEntityGoldenRecord.header.legalName, NamePartTypeV7.LegalName),
                legalEntityGoldenRecord.header.legalShortName?.let { NamePartV7Dto(it, NamePartTypeV7.ShortName) },
                legalEntityGoldenRecord.header.legalForm?.let { NamePartV7Dto(it, NamePartTypeV7.LegalForm) }
            ),
            owningCompany = owningCompany,
            uncategorized = UncategorizedPropertiesV7Dto.empty.copy(nameParts = nameParts, scriptVariants = scriptVariants),
            legalEntity = buildLegalEntityComponent(legalEntityGoldenRecord),
            site = buildSiteWithLegalAddressAsMainComponent(siteGoldenRecord),
            additionalAddress = null
        )
    }

    fun buildSiteBusinessPartner(
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseV7Dto,
        siteGoldenRecord: SiteWithMainAddressVerboseV7Dto,
        owningCompany: String?,
        nameParts: List<String>,
        scriptVariants: List<UncategorizedScriptVariantV7Dto> = emptyList()
    ): BusinessPartnerV7Dto{
        return BusinessPartnerV7Dto(
            nameParts = listOfNotNull(
                NamePartV7Dto(legalEntityGoldenRecord.header.legalName, NamePartTypeV7.LegalName),
                legalEntityGoldenRecord.header.legalShortName?.let { NamePartV7Dto(it, NamePartTypeV7.ShortName) },
                legalEntityGoldenRecord.header.legalForm?.let { NamePartV7Dto(it, NamePartTypeV7.LegalForm) },
                siteGoldenRecord.site.name.let { NamePartV7Dto(it, NamePartTypeV7.SiteName) },
            ),
            owningCompany = owningCompany,
            uncategorized = UncategorizedPropertiesV7Dto.empty.copy(nameParts = nameParts, scriptVariants = scriptVariants),
            legalEntity = buildLegalEntityComponent(legalEntityGoldenRecord),
            site = buildSiteComponent(siteGoldenRecord),
            additionalAddress = null
        )
    }

    fun buildAdditionLegalEntityAddressBusinessPartner(
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseV7Dto,
        addressGoldenRecord: LogisticAddressVerboseV7Dto,
        owningCompany: String?,
        nameParts: List<String>,
        scriptVariants: List<UncategorizedScriptVariantV7Dto> = emptyList()
    ): BusinessPartnerV7Dto {
        return BusinessPartnerV7Dto(
            nameParts = listOfNotNull(
                NamePartV7Dto(legalEntityGoldenRecord.header.legalName, NamePartTypeV7.LegalName),
                legalEntityGoldenRecord.header.legalShortName?.let { NamePartV7Dto(it, NamePartTypeV7.ShortName) },
                legalEntityGoldenRecord.header.legalForm?.let { NamePartV7Dto(it, NamePartTypeV7.LegalForm) },
                addressGoldenRecord.address.name?.let { NamePartV7Dto(it, NamePartTypeV7.AddressName) }
            ),
            owningCompany = owningCompany,
            uncategorized = UncategorizedPropertiesV7Dto.empty.copy(nameParts = nameParts, scriptVariants = scriptVariants),
            legalEntity = buildLegalEntityComponent(legalEntityGoldenRecord),
            site = null,
            additionalAddress = buildPostalAddressWithScriptVariants(addressGoldenRecord)
        )
    }

    fun buildAdditionSiteAddressBusinessPartner(
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseV7Dto,
        siteGoldenRecord: SiteWithMainAddressVerboseV7Dto,
        addressGoldenRecord: LogisticAddressVerboseV7Dto,
        owningCompany: String?,
        nameParts: List<String>,
        scriptVariants: List<UncategorizedScriptVariantV7Dto> = emptyList()
    ): BusinessPartnerV7Dto{
        return BusinessPartnerV7Dto(
            nameParts = listOfNotNull(
                NamePartV7Dto(legalEntityGoldenRecord.header.legalName, NamePartTypeV7.LegalName),
                legalEntityGoldenRecord.header.legalShortName?.let { NamePartV7Dto(it, NamePartTypeV7.ShortName) },
                legalEntityGoldenRecord.header.legalForm?.let { NamePartV7Dto(it, NamePartTypeV7.LegalForm) },
                siteGoldenRecord.site.name.let { NamePartV7Dto(it, NamePartTypeV7.SiteName) },
                addressGoldenRecord.address.name?.let { NamePartV7Dto(it, NamePartTypeV7.AddressName) }
            ),
            owningCompany = owningCompany,
            uncategorized = UncategorizedPropertiesV7Dto.empty.copy(nameParts = nameParts, scriptVariants = scriptVariants),
            legalEntity = buildLegalEntityComponent(legalEntityGoldenRecord),
            site = buildSiteComponent(siteGoldenRecord),
            additionalAddress = buildPostalAddressWithScriptVariants(addressGoldenRecord)
        )
    }

    private fun buildLegalEntityComponent(goldenRecord: LegalEntityWithLegalAddressVerboseV7Dto): LegalEntityV7Dto{
        return LegalEntityV7Dto(
            bpnReference = BpnReferenceV7Dto(goldenRecord.header.bpnl, null, BpnReferenceTypeV7.Bpn),
            legalName = goldenRecord.header.legalName,
            legalShortName = goldenRecord.header.legalShortName,
            legalForm = goldenRecord.header.legalForm,
            identifiers = goldenRecord.header.identifiers.map { IdentifierV7Dto(it.value, it.type, it.issuingBody) },
            states = goldenRecord.header.states.map { BusinessStateV7Dto(BusinessStateDates.receiveValidFrom(it.validFrom)?.toUtcInstant(), BusinessStateDates.receiveValidTo(it.validTo)?.toUtcInstant(), it.type) },
            confidenceCriteria = buildConfidence(goldenRecord.header.confidenceCriteria),
            isParticipantData = goldenRecord.header.isParticipantData,
            hasChanged = true,
            ownershipUltimate = goldenRecord.header.ownershipUltimate,
            ultimateOwnerBpnl = goldenRecord.header.ultimateOwnerBpnl,
            legalAddress = buildPostalAddress(goldenRecord.legalAddress),
            scriptVariants = goldenRecord.scriptVariants.map { buildLegalEntityScriptVariant(it) },
            updatedAt = goldenRecord.header.updatedAt
        )
    }

    private fun buildSiteComponent(goldenRecord: SiteWithMainAddressVerboseV7Dto): SiteV7Dto{
        return SiteV7Dto(
            bpnReference = BpnReferenceV7Dto(goldenRecord.site.bpns, null, BpnReferenceTypeV7.Bpn),
            siteName = goldenRecord.site.name,
            states = goldenRecord.site.states.map { BusinessStateV7Dto(BusinessStateDates.receiveValidFrom(it.validFrom)?.toUtcInstant(), BusinessStateDates.receiveValidTo(it.validTo)?.toUtcInstant(), it.type) },
            confidenceCriteria = buildConfidence(goldenRecord.site.confidenceCriteria),
            hasChanged = true,
            siteMainAddress = buildPostalAddress(goldenRecord.mainAddress),
            scriptVariants = goldenRecord.site.scriptVariants.map { buildSiteScriptVariant(it) },
            updatedAt = goldenRecord.site.updatedAt
        )
    }

    private fun buildSiteWithLegalAddressAsMainComponent(goldenRecord: SiteVerboseV7Dto): SiteV7Dto{
        return SiteV7Dto(
            bpnReference = BpnReferenceV7Dto(goldenRecord.bpns, null, BpnReferenceTypeV7.Bpn),
            siteName = goldenRecord.name,
            states = goldenRecord.states.map { BusinessStateV7Dto(BusinessStateDates.receiveValidFrom(it.validFrom)?.toUtcInstant(), BusinessStateDates.receiveValidTo(it.validTo)?.toUtcInstant(), it.type) },
            confidenceCriteria = buildConfidence(goldenRecord.confidenceCriteria),
            hasChanged = true,
            siteMainAddress = null,
            scriptVariants = goldenRecord.scriptVariants.map { buildSiteScriptVariant(it) },
            updatedAt = goldenRecord.updatedAt
        )
    }

    private fun buildConfidence(confidence: ConfidenceCriteriaV7Dto): OrchestratorConfidenceCriteriaV7Dto{
        return OrchestratorConfidenceCriteriaV7Dto(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            numberOfSharingMembers = confidence.numberOfSharingMembers,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant(),
            confidenceLevel = confidence.confidenceLevel
        )
    }

    private fun buildPostalAddressWithScriptVariants(logisticAddress: LogisticAddressVerboseV7Dto): PostalAddressWithScriptVariantsV7Dto{
        return PostalAddressWithScriptVariantsV7Dto(
            postalProperties = buildPostalAddress(logisticAddress.address),
            scriptVariants = logisticAddress.scriptVariants.map { buildPostalAddressScriptVariantWithScriptCode(it) }
        )
    }

    private fun buildPostalAddress(logisticAddress: LogisticAddressInvariantVerboseV7Dto): PostalAddressV7Dto{
        return PostalAddressV7Dto(
            bpnReference = BpnReferenceV7Dto(logisticAddress.bpna, null, BpnReferenceTypeV7.Bpn),
            hasChanged = true,
            addressName = logisticAddress.name,
            identifiers = logisticAddress.identifiers.map { IdentifierV7Dto(it.value, it.type, null) },
            states = logisticAddress.states.map {  BusinessStateV7Dto(BusinessStateDates.receiveValidFrom(it.validFrom)?.toUtcInstant(), BusinessStateDates.receiveValidTo(it.validTo)?.toUtcInstant(), it.type) },
            confidenceCriteria = buildConfidence(logisticAddress.confidenceCriteria),
            physicalAddress = buildPhysicalAddress(logisticAddress.physicalPostalAddress),
            alternativeAddress = logisticAddress.alternativePostalAddress?.let { buildAlternativeAddress(it) },
            updatedAt = logisticAddress.updatedAt
        )
    }

    private fun buildPhysicalAddress(physicalPostalAddress: PhysicalPostalAddressVerboseV7Dto): PhysicalAddressV7Dto{
        return PhysicalAddressV7Dto(
            geographicCoordinates = physicalPostalAddress.geographicCoordinates?.let { GeoCoordinateV7Dto(it.longitude, it.latitude, it.altitude) } ?: GeoCoordinateV7Dto.empty,
            country = physicalPostalAddress.country,
            administrativeAreaLevel1 = physicalPostalAddress.administrativeAreaLevel1,
            administrativeAreaLevel2 = physicalPostalAddress.administrativeAreaLevel2,
            administrativeAreaLevel3 = physicalPostalAddress.administrativeAreaLevel3,
            postalCode = physicalPostalAddress.postalCode,
            city = physicalPostalAddress.city,
            district = physicalPostalAddress.district,
            street = physicalPostalAddress.street?.let { buildStreet(it) } ?: OrchestratorStreetV7Dto.empty,
            companyPostalCode = physicalPostalAddress.companyPostalCode,
            industrialZone = physicalPostalAddress.industrialZone,
            building = physicalPostalAddress.building,
            floor = physicalPostalAddress.floor,
            door = physicalPostalAddress.door,
            taxJurisdictionCode = physicalPostalAddress.taxJurisdictionCode
        )
    }

    private fun buildAlternativeAddress(alternativeAddress: AlternativePostalAddressVerboseV7Dto): AlternativeAddressV7Dto{
        return AlternativeAddressV7Dto(
            geographicCoordinates = alternativeAddress.geographicCoordinates?.let { GeoCoordinateV7Dto(it.longitude, it.latitude, it.altitude) } ?: GeoCoordinateV7Dto.empty,
            country = alternativeAddress.country,
            administrativeAreaLevel1 =  alternativeAddress.administrativeAreaLevel1,
            postalCode = alternativeAddress.postalCode,
            city = alternativeAddress.city,
            deliveryServiceNumber = alternativeAddress.deliveryServiceNumber,
            deliveryServiceType = alternativeAddress.deliveryServiceType,
            deliveryServiceQualifier = alternativeAddress.deliveryServiceQualifier
        )
    }

    private fun buildStreet(street: StreetV7Dto): OrchestratorStreetV7Dto{
        return OrchestratorStreetV7Dto(
            name = street.name,
            houseNumber = street.houseNumber,
            houseNumberSupplement = street.houseNumberSupplement,
            milestone = street.milestone,
            direction = street.direction,
            namePrefix = street.namePrefix,
            nameSuffix = street.nameSuffix,
            additionalNamePrefix = street.additionalNamePrefix,
            additionalNameSuffix = street.additionalNameSuffix
        )
    }

    private fun buildLegalEntityScriptVariant(legalEntityScriptVariantDto: LegalEntityScriptVariantV7Dto): OrchestratorLegalEntityScriptVariantV7Dto{
        return with(legalEntityScriptVariantDto){
            OrchestratorLegalEntityScriptVariantV7Dto(
                scriptCode = scriptCode,
                legalName = legalName,
                legalShortName = shortName,
                legalAddress = buildPostalAddressScriptVariant(legalAddress)
            )
        }
    }

    private fun buildSiteScriptVariant(siteScriptVariant: SiteScriptVariantV7Dto): OrchestratorSiteScriptVariantV7Dto{
        return with(siteScriptVariant){
            OrchestratorSiteScriptVariantV7Dto(
                scriptCode = scriptCode,
                siteName = requireNotNull(name) { "A site script variant without a name cannot become a golden record task result" },
                mainAddress = buildPostalAddressScriptVariant(mainAddress)
            )
        }
    }

    private fun buildPostalAddressScriptVariantWithScriptCode(logisticAddressScriptVariant: LogisticAddressScriptVariantV7Dto): PostalAddressScriptVariantWithScriptCodeV7Dto{
        return with(logisticAddressScriptVariant){
            PostalAddressScriptVariantWithScriptCodeV7Dto(scriptCode, buildPostalAddressScriptVariant(logisticAddressScriptVariant.address))
        }
    }

    private fun buildPostalAddressScriptVariant(postalAddressScriptVariant: PostalAddressScriptVariantV7Dto): OrchestratorPostalAddressScriptVariantV7Dto{
        return with(postalAddressScriptVariant){
            OrchestratorPostalAddressScriptVariantV7Dto(
                addressName = addressName,
                physicalAddress = buildPhysicalAddressScriptVariant(physicalAddress),
                alternativeAddress = alternativeAddress?.let { buildAlternativeAddressScriptVariant(it) }
            )
        }
    }

    private fun buildPhysicalAddressScriptVariant(physicalAddressScriptVariant: PhysicalAddressScriptVariantV7Dto): OrchestratorPhysicalAddressScriptVariantV7Dto{
        return with(physicalAddressScriptVariant){
            OrchestratorPhysicalAddressScriptVariantV7Dto(
                city = city,
                district = district,
                street = street?.let { buildScriptVariantStreet(it) } ?: OrchestratorStreetScriptVariantV7Dto.empty,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door)
        }
    }

    private fun buildAlternativeAddressScriptVariant(alternativeAddressScriptVariant: AlternativeAddressScriptVariantV7Dto): OrchestratorAlternativeAddressScriptVariantV7Dto{
        return with(alternativeAddressScriptVariant){
            OrchestratorAlternativeAddressScriptVariantV7Dto(
                city = city
            )
        }
    }

    private fun buildScriptVariantStreet(street: StreetScriptVariantV7Dto): OrchestratorStreetScriptVariantV7Dto{
        return with(street){
            OrchestratorStreetScriptVariantV7Dto(
                name = name,
                direction = direction,
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix
            )
        }
    }

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)
}