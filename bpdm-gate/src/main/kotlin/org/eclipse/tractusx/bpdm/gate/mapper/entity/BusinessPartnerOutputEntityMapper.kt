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

package org.eclipse.tractusx.bpdm.gate.mapper.entity

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AlternativeAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordRelationRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRelationType
import org.springframework.stereotype.Component

/**
 * Maps business partner output data to the output entity that stores it.
 */
@Component
class BusinessPartnerOutputEntityMapper(
    private val inputEntityMapper: BusinessPartnerInputEntityMapper
) {

    /**
     * Returns the output entity of [sharingState] holding [upsertData].
     */
    fun toEntity(upsertData: BusinessPartnerOutputContentParsed, sharingState: SharingStateDb): BusinessPartnerDb {
        val businessPartner =  with(upsertData) {
            BusinessPartnerDb(
                sharingState = sharingState,
                stage = StageType.Output,
                nameParts = nameParts.toMutableList(),
                roles = roles.toSortedSet(),
                identifiers = identifiers.map { it.toEntity() }.toSortedSet(),
                states = states.map { it.toEntity() }.toSortedSet(),
                shortName = shortName,
                legalName = legalName,
                siteName = siteName,
                addressName = addressName,
                legalForm = legalForm,
                isOwnCompanyData = isOwnCompanyData,
                bpnL = legalEntityBpn,
                bpnS = siteBpn,
                bpnA = addressBpn,
                ownershipUltimate = ownershipUltimate,
                ultimateOwnerBpnl = ultimateOwnerBpnl,
                postalAddress = PostalAddressDb(addressType, physicalPostalAddress.toEntity(), alternativePostalAddress?.toEntity()),
                legalEntityConfidence = legalEntityConfidence.toEntity(),
                siteConfidence = siteConfidence?.toEntity(),
                addressConfidence = addressConfidence.toEntity(),
                legalEntityUpdatedAt = legalEntityUpdatedAt,
                siteUpdatedAt = siteUpdatedAt,
                addressUpdatedAt = addressUpdatedAt
            )
        }

        upsertData.scriptVariants.map { toScriptVariant(businessPartner, it) }
            .forEach { businessPartner.scriptVariants.add(it) }

        upsertData.legalEntityGoldenRecordRelations.map { toLegalEntityGoldenRecordRelation(it) }
            .forEach { businessPartner.legalEntityGoldenRecordRelations.add(it) }
        upsertData.siteGoldenRecordRelations.map { toSiteGoldenRecordRelation(it) }
            .forEach { businessPartner.siteGoldenRecordRelations.add(it) }
        upsertData.addressGoldenRecordRelations.map { toAddressGoldenRecordRelation(it) }
            .forEach { businessPartner.addressGoldenRecordRelations.add(it) }
        upsertData.additionalSites.map { AdditionalSiteDb(it.siteBpn, it.name) }
            .forEach { businessPartner.additionalSites.add(it) }

        return businessPartner
    }

    private fun toScriptVariant(businessPartner: BusinessPartnerDb, scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        BusinessPartnerScriptVariantDb(
            scriptCode = scriptVariant.scriptCode,
            businessPartner = businessPartner,
            nameParts = scriptVariant.nameParts.toMutableList(),
            shortName = scriptVariant.shortName,
            legalName = scriptVariant.legalName,
            siteName = scriptVariant.siteName,
            addressName = scriptVariant.addressName,
            physicalAddress = inputEntityMapper.toEntity(scriptVariant.physicalAddress),
            alternativeAddress = scriptVariant.alternativeAddressCity?.let(inputEntityMapper::toAlternativeAddressScriptVariant)
        )

    private fun toLegalEntityGoldenRecordRelation(relation: GoldenRecordRelationRaw<LegalEntityGoldenRecordRelationType>) =
        LegalEntityGoldenRecordRelationDb(
            relationType = relation.type,
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    private fun toSiteGoldenRecordRelation(relation: GoldenRecordRelationRaw<SiteGoldenRecordRelationType>) =
        SiteGoldenRecordRelationDb(
            relationType = relation.type,
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    private fun toAddressGoldenRecordRelation(relation: GoldenRecordRelationRaw<AddressGoldenRecordRelationType>) =
        AddressGoldenRecordRelationDb(
            relationType = relation.type,
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    private fun IdentifierRaw.toEntity() =
        IdentifierDb(type = type, value = value, issuingBody = issuingBody, businessPartnerType = businessPartnerType)

    private fun StateRaw.toEntity() =
        StateDb(type = type, validFrom = validFrom, validTo = validTo, businessPartnerTyp = businessPartnerType)

    private fun ConfidenceCriteriaRaw.toEntity() =
        ConfidenceCriteriaDb(sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers, lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel)


    private fun PhysicalPostalAddressRaw.toEntity() =
        PhysicalPostalAddressDb(
            geographicCoordinates = geographicCoordinates?.toEntity(),
            country = country,
            administrativeAreaLevel1 = administrativeAreaLevel1,
            administrativeAreaLevel2 = administrativeAreaLevel2,
            administrativeAreaLevel3 = administrativeAreaLevel3,
            postalCode = postalCode,
            city = city,
            district = district,
            street = street?.toEntity(),
            companyPostalCode = companyPostalCode,
            industrialZone = industrialZone,
            building = building,
            floor = floor,
            door = door,
            taxJurisdictionCode = taxJurisdictionCode
        )

    private fun AlternativeAddressRaw.toEntity() =
        AlternativePostalAddressDb(
            geographicCoordinates = geographicCoordinates?.toEntity(),
            country = country,
            administrativeAreaLevel1 = administrativeAreaLevel1,
            postalCode = postalCode,
            city = city,
            deliveryServiceType = deliveryServiceType,
            deliveryServiceQualifier = deliveryServiceQualifier,
            deliveryServiceNumber = deliveryServiceNumber
        )

    private fun StreetRaw.toEntity() =
        StreetDb(
            name = name,
            houseNumber = houseNumber,
            houseNumberSupplement = houseNumberSupplement,
            milestone = milestone,
            direction = direction,
            namePrefix = namePrefix,
            additionalNamePrefix = additionalNamePrefix,
            nameSuffix = nameSuffix,
            additionalNameSuffix = additionalNameSuffix
        )

    private fun GeoCoordinateRaw.toEntity() = GeographicCoordinateDb(latitude = latitude, longitude = longitude, altitude = altitude)

}