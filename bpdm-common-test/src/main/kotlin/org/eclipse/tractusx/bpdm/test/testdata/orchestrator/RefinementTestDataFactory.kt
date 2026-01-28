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

import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.orchestrator.api.model.*
import java.time.LocalDateTime
import java.time.ZoneOffset

class RefinementTestDataFactory {

    fun buildLegalEntityBusinessPartner(
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseDto,
        owningCompany: String?,
        nameParts: List<String>
    ): BusinessPartner{
        return BusinessPartner(
            nameParts = listOfNotNull(
                NamePart(legalEntityGoldenRecord.legalEntity.legalName, NamePartType.LegalName),
                legalEntityGoldenRecord.legalEntity.legalShortName?.let { NamePart(it, NamePartType.ShortName) },
                legalEntityGoldenRecord.legalEntity.legalForm?.let { NamePart(it, NamePartType.LegalForm) }
            ),
            owningCompany = owningCompany,
            uncategorized = UncategorizedProperties.empty.copy(nameParts = nameParts),
            legalEntity = buildLegalEntityComponent(legalEntityGoldenRecord),
            site = null,
            additionalAddress = null
        )
    }


    fun buildLegalEntityOnSiteBusinessPartner(
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseDto,
        siteGoldenRecord: SiteVerboseDto,
        owningCompany: String?,
        nameParts: List<String>
    ): BusinessPartner{
        return BusinessPartner(
            nameParts = listOfNotNull(
                NamePart(legalEntityGoldenRecord.legalEntity.legalName, NamePartType.LegalName),
                legalEntityGoldenRecord.legalEntity.legalShortName?.let { NamePart(it, NamePartType.ShortName) },
                legalEntityGoldenRecord.legalEntity.legalForm?.let { NamePart(it, NamePartType.LegalForm) }
            ),
            owningCompany = owningCompany,
            uncategorized = UncategorizedProperties.empty.copy(nameParts = nameParts),
            legalEntity = buildLegalEntityComponent(legalEntityGoldenRecord),
            site = buildSiteWithLegalAddressAsMainComponent(siteGoldenRecord),
            additionalAddress = null
        )
    }

    fun buildSiteBusinessPartner(
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseDto,
        siteGoldenRecord: SiteWithMainAddressVerboseDto,
        owningCompany: String?,
        nameParts: List<String>
    ): BusinessPartner{
        return BusinessPartner(
            nameParts = listOfNotNull(
                NamePart(legalEntityGoldenRecord.legalEntity.legalName, NamePartType.LegalName),
                legalEntityGoldenRecord.legalEntity.legalShortName?.let { NamePart(it, NamePartType.ShortName) },
                legalEntityGoldenRecord.legalEntity.legalForm?.let { NamePart(it, NamePartType.LegalForm) },
                siteGoldenRecord.site.name.let { NamePart(it, NamePartType.SiteName) },
            ),
            owningCompany = owningCompany,
            uncategorized = UncategorizedProperties.empty.copy(nameParts = nameParts),
            legalEntity = buildLegalEntityComponent(legalEntityGoldenRecord),
            site = buildSiteComponent(siteGoldenRecord),
            additionalAddress = null
        )
    }

    fun buildAdditionSiteAddressBusinessPartner(
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseDto,
        siteGoldenRecord: SiteWithMainAddressVerboseDto,
        addressGoldenRecord: LogisticAddressVerboseDto,
        owningCompany: String?,
        nameParts: List<String>
    ): BusinessPartner{
        return BusinessPartner(
            nameParts = listOfNotNull(
                NamePart(legalEntityGoldenRecord.legalEntity.legalName, NamePartType.LegalName),
                legalEntityGoldenRecord.legalEntity.legalShortName?.let { NamePart(it, NamePartType.ShortName) },
                legalEntityGoldenRecord.legalEntity.legalForm?.let { NamePart(it, NamePartType.LegalForm) },
                siteGoldenRecord.site.name.let { NamePart(it, NamePartType.SiteName) },
                addressGoldenRecord.name?.let { NamePart(it, NamePartType.AddressName) }
            ),
            owningCompany = owningCompany,
            uncategorized = UncategorizedProperties.empty.copy(nameParts = nameParts),
            legalEntity = buildLegalEntityComponent(legalEntityGoldenRecord),
            site = buildSiteComponent(siteGoldenRecord),
            additionalAddress = buildPostalAddress(addressGoldenRecord)
        )
    }

    private fun buildLegalEntityComponent(goldenRecord: LegalEntityWithLegalAddressVerboseDto): LegalEntity{
        return LegalEntity(
            bpnReference = BpnReference(goldenRecord.legalEntity.bpnl, null, BpnReferenceType.Bpn),
            legalName = goldenRecord.legalEntity.legalName,
            legalShortName = goldenRecord.legalEntity.legalShortName,
            legalForm = goldenRecord.legalEntity.legalForm,
            identifiers = goldenRecord.legalEntity.identifiers.map { Identifier(it.value, it.type, it.issuingBody) },
            states = goldenRecord.legalEntity.states.map { BusinessState(it.validFrom?.toUtcInstant(), it.validTo?.toUtcInstant(), it.type) },
            confidenceCriteria = buildConfidence(goldenRecord.legalEntity.confidenceCriteria),
            isParticipantData = goldenRecord.legalEntity.isParticipantData,
            hasChanged = true,
            legalAddress = buildPostalAddress(goldenRecord.legalAddress)
        )
    }

    private fun buildSiteComponent(goldenRecord: SiteWithMainAddressVerboseDto): Site{
        return Site(
            bpnReference = BpnReference(goldenRecord.site.bpns, null, BpnReferenceType.Bpn),
            siteName = goldenRecord.site.name,
            states = goldenRecord.site.states.map { BusinessState(it.validFrom?.toUtcInstant(), it.validTo?.toUtcInstant(), it.type) },
            confidenceCriteria = buildConfidence(goldenRecord.site.confidenceCriteria),
            hasChanged = true,
            siteMainAddress = buildPostalAddress(goldenRecord.mainAddress)
        )
    }

    private fun buildSiteWithLegalAddressAsMainComponent(goldenRecord: SiteVerboseDto): Site{
        return Site(
            bpnReference = BpnReference(goldenRecord.bpns, null, BpnReferenceType.Bpn),
            siteName = goldenRecord.name,
            states = goldenRecord.states.map { BusinessState(it.validFrom?.toUtcInstant(), it.validTo?.toUtcInstant(), it.type) },
            confidenceCriteria = buildConfidence(goldenRecord.confidenceCriteria),
            hasChanged = true,
            siteMainAddress = null
        )
    }

    private fun buildConfidence(confidence: ConfidenceCriteriaDto): ConfidenceCriteria{
        return ConfidenceCriteria(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            numberOfSharingMembers = confidence.numberOfSharingMembers,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant(),
            confidenceLevel = confidence.confidenceLevel
        )
    }

    private fun buildPostalAddress(logisticAddress: LogisticAddressVerboseDto): PostalAddress{
        return PostalAddress(
            bpnReference = BpnReference(logisticAddress.bpna, null, BpnReferenceType.Bpn),
            hasChanged = true,
            addressName = logisticAddress.name,
            identifiers = logisticAddress.identifiers.map { Identifier(it.value, it.type, null) },
            states = logisticAddress.states.map {  BusinessState(it.validFrom?.toUtcInstant(), it.validTo?.toUtcInstant(), it.type) },
            confidenceCriteria = buildConfidence(logisticAddress.confidenceCriteria),
            physicalAddress = buildPhysicalAddress(logisticAddress.physicalPostalAddress),
            alternativeAddress = logisticAddress.alternativePostalAddress?.let { buildAlternativeAddress(it) }
        )
    }

    private fun buildPhysicalAddress(physicalPostalAddress: PhysicalPostalAddressVerboseDto): PhysicalAddress{
        return PhysicalAddress(
            geographicCoordinates = physicalPostalAddress.geographicCoordinates?.let { GeoCoordinate(it.longitude, it.latitude, it.altitude) } ?: GeoCoordinate.empty,
            country = physicalPostalAddress.country.alpha2,
            administrativeAreaLevel1 = physicalPostalAddress.administrativeAreaLevel1,
            administrativeAreaLevel2 = physicalPostalAddress.administrativeAreaLevel2,
            administrativeAreaLevel3 = physicalPostalAddress.administrativeAreaLevel3,
            postalCode = physicalPostalAddress.postalCode,
            city = physicalPostalAddress.city,
            district = physicalPostalAddress.district,
            street = physicalPostalAddress.street?.let { buildStreet(it) } ?: Street.empty,
            companyPostalCode = physicalPostalAddress.companyPostalCode,
            industrialZone = physicalPostalAddress.industrialZone,
            building = physicalPostalAddress.building,
            floor = physicalPostalAddress.floor,
            door = physicalPostalAddress.door,
            taxJurisdictionCode = physicalPostalAddress.taxJurisdictionCode
        )
    }

    private fun buildAlternativeAddress(alternativeAddress: AlternativePostalAddressVerboseDto): AlternativeAddress{
        return AlternativeAddress(
            geographicCoordinates = alternativeAddress.geographicCoordinates?.let { GeoCoordinate(it.longitude, it.latitude, it.altitude) } ?: GeoCoordinate.empty,
            country = alternativeAddress.country.alpha2,
            administrativeAreaLevel1 =  alternativeAddress.administrativeAreaLevel1,
            postalCode = alternativeAddress.postalCode,
            city = alternativeAddress.city,
            deliveryServiceNumber = alternativeAddress.deliveryServiceNumber,
            deliveryServiceType = alternativeAddress.deliveryServiceType,
            deliveryServiceQualifier = alternativeAddress.deliveryServiceQualifier
        )
    }

    private fun buildStreet(street: StreetDto): Street{
        return Street(
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

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)
}