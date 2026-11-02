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

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.orchestrator.api.model.Site
import org.eclipse.tractusx.orchestrator.api.model.UncategorizedProperties
import org.eclipse.tractusx.orchestrator.api.v6.model.AlternativeAddressV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BpnReferenceTypeV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BpnReferenceV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessStateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartnerV6
import org.eclipse.tractusx.orchestrator.api.v6.model.ConfidenceCriteriaV6
import org.eclipse.tractusx.orchestrator.api.v6.model.GeoCoordinateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.IdentifierV6
import org.eclipse.tractusx.orchestrator.api.v6.model.LegalEntityV6
import org.eclipse.tractusx.orchestrator.api.v6.model.NamePartTypeV6
import org.eclipse.tractusx.orchestrator.api.v6.model.NamePartV6
import org.eclipse.tractusx.orchestrator.api.v6.model.PhysicalAddressV6
import org.eclipse.tractusx.orchestrator.api.v6.model.PostalAddressV6
import org.eclipse.tractusx.orchestrator.api.v6.model.SiteV6
import org.eclipse.tractusx.orchestrator.api.v6.model.StreetV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequestEntryV6
import org.eclipse.tractusx.orchestrator.api.v6.model.UncategorizedPropertiesV6
import java.util.*
import kotlin.random.Random

class OrchestratorRequestFactoryV6(
    private val commonFactory: OrchestratorRequestFactoryCommon
) {

    fun buildTaskCreate(seed: String): TaskCreateRequestEntryV6{
        return TaskCreateRequestEntryV6(
            recordId = UUID.randomUUID().toString(),
            businessPartner = buildBusinessPartner(seed)
        )
    }


    fun buildBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV6{
        return BusinessPartnerV6(
            nameParts = commonFactory.buildNameParts(seed).map(::toNamePartV6),
            owningCompany = "BPNLOwner",
            uncategorized = toUncategorizedPropertiesV6(commonFactory.buildUncategorizedProperties(seed, random)),
            legalEntity = buildLegalEntityProperties(seed, random),
            site = toSiteV6(commonFactory.buildSite(seed, random)),
            additionalAddress = toPostalAddressV6(commonFactory.buildPostalAddress(seed, AddressType.AdditionalAddress, random))
        )
    }

    fun buildLegalEntityBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV6{
        return BusinessPartnerV6(
            nameParts = commonFactory.buildNameParts(seed).map(::toNamePartV6),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedPropertiesV6.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = null,
            additionalAddress = null
        )
    }

    fun buildSiteBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV6{
        return BusinessPartnerV6(
            nameParts = commonFactory.buildNameParts(seed).map(::toNamePartV6),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedPropertiesV6.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = toSiteV6(commonFactory.buildSite(seed, random)),
            additionalAddress = null
        )
    }

    fun buildLegalAddressSiteBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV6{
        return BusinessPartnerV6(
            nameParts = commonFactory.buildNameParts(seed).map(::toNamePartV6),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedPropertiesV6.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = toSiteV6(commonFactory.buildSite(seed, random).copy(siteMainAddress = null)),
            additionalAddress = null
        )
    }

    fun buildLegalEntityAdditionalAddressBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV6{
        return BusinessPartnerV6(
            nameParts = commonFactory.buildNameParts(seed).map(::toNamePartV6),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedPropertiesV6.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = null,
            additionalAddress = toPostalAddressV6(commonFactory.buildPostalAddress(seed, AddressType.AdditionalAddress, random))
        )
    }

    fun buildSiteAdditionalAddressBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV6{
        return BusinessPartnerV6(
            nameParts = commonFactory.buildNameParts(seed).map(::toNamePartV6),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedPropertiesV6.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = toSiteV6(commonFactory.buildSite(seed, random)),
            additionalAddress = toPostalAddressV6(commonFactory.buildPostalAddress(seed, AddressType.AdditionalAddress, random))
        )
    }


    fun buildLegalEntityProperties(seed: String, random: Random = commonFactory.createRandomFromSeed(seed)): LegalEntityV6 {
        return LegalEntityV6(
            bpnReference = toBpnReferenceV6(commonFactory.buildBpnLReference(seed)),
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = commonFactory.metadata?.legalForms?.random(random) ?: "Legal Form $seed",
            identifiers = commonFactory.buildLegalIdentifiers(seed, random).map(::toIdentifierV6),
            states = commonFactory.buildStates(random).map(::toBusinessStateV6),
            confidenceCriteria = toConfidenceCriteriaV6(commonFactory.buildConfidenceCriteria(random)),
            isCatenaXMemberData = random.nextBoolean(),
            hasChanged = random.nextBoolean(),
            legalAddress = toPostalAddressV6(commonFactory.buildPostalAddress(seed, AddressType.LegalAddress, random))
        )
    }


    private fun createRandomFromSeed(seed: String): Random{
        return Random(seed.hashCode())
    }

    private fun toNamePartV6(namePart: org.eclipse.tractusx.orchestrator.api.model.NamePart) =
        NamePartV6(
            name = namePart.name,
            type = when (namePart.type) {
                org.eclipse.tractusx.orchestrator.api.model.NamePartType.LegalName -> NamePartTypeV6.LegalName
                org.eclipse.tractusx.orchestrator.api.model.NamePartType.ShortName -> NamePartTypeV6.ShortName
                org.eclipse.tractusx.orchestrator.api.model.NamePartType.LegalForm -> NamePartTypeV6.LegalForm
                org.eclipse.tractusx.orchestrator.api.model.NamePartType.SiteName -> NamePartTypeV6.SiteName
                org.eclipse.tractusx.orchestrator.api.model.NamePartType.AddressName -> NamePartTypeV6.AddressName
            }
        )

    private fun toUncategorizedPropertiesV6(uncategorizedProperties: UncategorizedProperties) =
        UncategorizedPropertiesV6(
            nameParts = uncategorizedProperties.nameParts,
            identifiers = uncategorizedProperties.identifiers.map(::toIdentifierV6),
            states = uncategorizedProperties.states.map(::toBusinessStateV6),
            address = uncategorizedProperties.address?.postalProperties?.let(::toPostalAddressV6)
        )

    private fun toSiteV6(site: Site) =
        SiteV6(
            bpnReference = toBpnReferenceV6(site.bpnReference),
            siteName = site.siteName,
            states = site.states.map(::toBusinessStateV6),
            confidenceCriteria = toConfidenceCriteriaV6(site.confidenceCriteria),
            hasChanged = site.hasChanged,
            siteMainAddress = site.siteMainAddress?.let(::toPostalAddressV6)
        )

    private fun toPostalAddressV6(postalAddress: org.eclipse.tractusx.orchestrator.api.model.PostalAddress) =
        PostalAddressV6(
            bpnReference = toBpnReferenceV6(postalAddress.bpnReference),
            addressName = postalAddress.addressName,
            identifiers = postalAddress.identifiers.map(::toIdentifierV6),
            states = postalAddress.states.map(::toBusinessStateV6),
            confidenceCriteria = toConfidenceCriteriaV6(postalAddress.confidenceCriteria),
            physicalAddress = PhysicalAddressV6(
                geographicCoordinates = GeoCoordinateV6(
                    postalAddress.physicalAddress.geographicCoordinates.longitude,
                    postalAddress.physicalAddress.geographicCoordinates.latitude,
                    postalAddress.physicalAddress.geographicCoordinates.altitude
                ),
                country = postalAddress.physicalAddress.country,
                administrativeAreaLevel1 = postalAddress.physicalAddress.administrativeAreaLevel1,
                administrativeAreaLevel2 = postalAddress.physicalAddress.administrativeAreaLevel2,
                administrativeAreaLevel3 = postalAddress.physicalAddress.administrativeAreaLevel3,
                postalCode = postalAddress.physicalAddress.postalCode,
                city = postalAddress.physicalAddress.city,
                district = postalAddress.physicalAddress.district,
                street = StreetV6(
                    postalAddress.physicalAddress.street.name,
                    postalAddress.physicalAddress.street.houseNumber,
                    postalAddress.physicalAddress.street.houseNumberSupplement,
                    postalAddress.physicalAddress.street.milestone,
                    postalAddress.physicalAddress.street.direction,
                    postalAddress.physicalAddress.street.namePrefix,
                    postalAddress.physicalAddress.street.additionalNamePrefix,
                    postalAddress.physicalAddress.street.nameSuffix,
                    postalAddress.physicalAddress.street.additionalNameSuffix
                ),
                companyPostalCode = postalAddress.physicalAddress.companyPostalCode,
                industrialZone = postalAddress.physicalAddress.industrialZone,
                building = postalAddress.physicalAddress.building,
                floor = postalAddress.physicalAddress.floor,
                door = postalAddress.physicalAddress.door,
                taxJurisdictionCode = postalAddress.physicalAddress.taxJurisdictionCode
            ),
            alternativeAddress = postalAddress.alternativeAddress?.let {
                AlternativeAddressV6(
                    geographicCoordinates = GeoCoordinateV6(
                        it.geographicCoordinates.longitude,
                        it.geographicCoordinates.latitude,
                        it.geographicCoordinates.altitude
                    ),
                    country = it.country,
                    administrativeAreaLevel1 = it.administrativeAreaLevel1,
                    postalCode = it.postalCode,
                    city = it.city,
                    deliveryServiceType = it.deliveryServiceType,
                    deliveryServiceQualifier = it.deliveryServiceQualifier,
                    deliveryServiceNumber = it.deliveryServiceNumber
                )
            },
            hasChanged = postalAddress.hasChanged
        )

    private fun toBpnReferenceV6(bpnReference: org.eclipse.tractusx.orchestrator.api.model.BpnReference) =
        BpnReferenceV6(
            bpnReference.referenceValue,
            bpnReference.desiredBpn,
            bpnReference.referenceType?.let {
                when (it) {
                    org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.Bpn -> BpnReferenceTypeV6.Bpn
                    org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.BpnRequestIdentifier -> BpnReferenceTypeV6.BpnRequestIdentifier
                }
            }
        )

    private fun toBusinessStateV6(businessState: org.eclipse.tractusx.orchestrator.api.model.BusinessState) =
        BusinessStateV6(businessState.validFrom, businessState.validTo, businessState.type)

    private fun toIdentifierV6(identifier: org.eclipse.tractusx.orchestrator.api.model.Identifier) =
        IdentifierV6(identifier.value, identifier.type, identifier.issuingBody)

    private fun toConfidenceCriteriaV6(confidenceCriteria: org.eclipse.tractusx.orchestrator.api.model.ConfidenceCriteria) =
        ConfidenceCriteriaV6(
            confidenceCriteria.sharedByOwner,
            confidenceCriteria.checkedByExternalDataSource,
            confidenceCriteria.numberOfSharingMembers,
            confidenceCriteria.lastConfidenceCheckAt,
            confidenceCriteria.nextConfidenceCheckAt,
            confidenceCriteria.confidenceLevel
        )
}