/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.mapper

import org.eclipse.tractusx.bpdm.pool.entity.AddressIdentifierDb
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.AddressStateDb
import org.eclipse.tractusx.bpdm.pool.entity.AlternativePostalAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.BpnIdentifierMatchDb
import org.eclipse.tractusx.bpdm.pool.entity.BpnRequestIdentifierMappingDb
import org.eclipse.tractusx.bpdm.pool.entity.ConfidenceCriteriaDb
import org.eclipse.tractusx.bpdm.pool.entity.CountryDb
import org.eclipse.tractusx.bpdm.pool.entity.FieldQualityRuleDb
import org.eclipse.tractusx.bpdm.pool.entity.GeographicCoordinateDb
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityIdentifierDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityStateDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressScriptVariantDb
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntryDb
import org.eclipse.tractusx.bpdm.pool.entity.PhysicalAddressScriptVariantDb
import org.eclipse.tractusx.bpdm.pool.entity.PhysicalPostalAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.ReasonCodeDb
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.ScriptCodeDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteStateDb
import org.eclipse.tractusx.bpdm.pool.entity.StreetDb
import org.eclipse.tractusx.bpdm.pool.entity.StreetScriptVariantDb
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressIdentifierRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AlternativeAddressScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AlternativePostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.GeoCoordinateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.PhysicalAddressScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.PhysicalPostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.StreetRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.StreetScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.bpn.BpnIdentifierMatchRecord
import org.eclipse.tractusx.bpdm.pool.model.output.bpn.BpnRequestIdentifierMatchRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessPartnerSearchAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessPartnerSearchLegalEntityRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessPartnerSearchRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessPartnerSearchSiteRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.ConfidenceCriteriaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.RelationValidityPeriodRecord
import org.eclipse.tractusx.bpdm.pool.model.output.changelog.ChangelogEntryRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityIdentifierRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.AdministrativeAreaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.CountryRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.FieldQualityRuleRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.IdentifierTypeDetailRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.IdentifierTypeRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.LegalFormRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.ReasonCodeRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.ScriptCodeRecord
import org.eclipse.tractusx.bpdm.pool.model.output.participation.DataSpaceParticipantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteScriptVariantRecord
import org.springframework.stereotype.Component

/**
 * Maps persisted entities to the output records a use case hands back.
 */
@Component
class DbToOutputMapper {

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: ReasonCodeDb): ReasonCodeRecord =
        ReasonCodeRecord(
            technicalKey = entity.technicalKey,
            description = entity.description
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: ScriptCodeDb): ScriptCodeRecord =
        ScriptCodeRecord(
            technicalKey = entity.technicalKey,
            description = entity.description
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: CountryDb): CountryRecord =
        CountryRecord(
            countryCode = entity.countryCode,
            name = entity.name,
            description = entity.description
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: RegionDb): AdministrativeAreaRecord =
        AdministrativeAreaRecord(
            countryCode = entity.country.countryCode,
            code = entity.regionCode,
            name = entity.regionName
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: LegalFormDb): LegalFormRecord =
        LegalFormRecord(
            technicalKey = entity.technicalKey,
            name = entity.name,
            transliteratedName = entity.transliteratedName,
            abbreviation = entity.abbreviation,
            transliteratedAbbreviations = entity.transliteratedAbbreviations,
            countryCode = entity.country?.countryCode,
            languageCode = entity.languageCode,
            administrativeAreaCode = entity.administrativeArea?.regionCode,
            isActive = entity.isActive
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: IdentifierTypeDb): IdentifierTypeRecord =
        IdentifierTypeRecord(
            technicalKey = entity.technicalKey,
            businessPartnerType = entity.businessPartnerType,
            name = entity.name,
            abbreviation = entity.abbreviation,
            transliteratedName = entity.transliteratedName,
            transliteratedAbbreviation = entity.transliteratedAbbreviation,
            format = entity.format,
            categories = entity.categories.toSet(),
            details = entity.details.map { IdentifierTypeDetailRecord(countryCode = it.country?.countryCode, mandatory = it.mandatory) }
        )

    /**
     * Returns the output record for [entity] as it applies in [country].
     */
    fun toRecord(entity: FieldQualityRuleDb, country: CountryDb): FieldQualityRuleRecord =
        FieldQualityRuleRecord(
            fieldPath = entity.fieldPath,
            schemaName = entity.schemaName,
            countryCode = country.countryCode,
            qualityLevel = entity.qualityLevel
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: BpnIdentifierMatchDb): BpnIdentifierMatchRecord =
        BpnIdentifierMatchRecord(
            identifierValue = entity.identifierValue,
            bpn = entity.bpn
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: BpnRequestIdentifierMappingDb): BpnRequestIdentifierMatchRecord =
        BpnRequestIdentifierMatchRecord(
            requestIdentifier = entity.requestIdentifier,
            bpn = entity.bpn
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: PartnerChangelogEntryDb): ChangelogEntryRecord =
        ChangelogEntryRecord(
            bpn = entity.bpn,
            businessPartnerType = entity.businessPartnerType,
            timestamp = entity.updatedAt.instant,
            changelogType = entity.changelogType
        )

    /**
     * Returns the data space participation [entity] holds.
     */
    fun toParticipantRecord(entity: LegalEntityDb): DataSpaceParticipantRecord =
        DataSpaceParticipantRecord(legalEntityBpn = entity.bpn, isDataSpaceParticipant = entity.isDataSpaceParticipant)

    /**
     * Returns the output record for [entity], together with its legal address.
     */
    fun toRecord(entity: LegalEntityDb): LegalEntityRecord =
        LegalEntityRecord(
            bpn = entity.bpn,
            legalName = entity.legalName.value,
            legalShortName = entity.legalName.shortName,
            legalForm = entity.legalForm?.let { toRecord(it) },
            identifiers = entity.identifiers.map { toIdentifierRecord(it) },
            states = entity.states.map { toStateRecord(it) },
            relations = entity.startNodeRelations.plus(entity.endNodeRelations).map { toRelationRecord(it) },
            currentness = entity.currentness,
            confidenceCriteria = toConfidenceCriteriaRecord(entity.confidenceCriteria),
            isDataSpaceParticipant = entity.isDataSpaceParticipant,
            createdAt = entity.createdAt.instant,
            updatedAt = entity.updatedAt.instant,
            ownershipUltimate = entity.ownershipUltimate,
            ultimateOwnerBpnl = entity.ultimateOwnerBpnl,
            legalAddress = toRecord(entity.legalAddress),
            scriptVariants = toLegalEntityScriptVariantRecords(entity)
        )

    /**
     * Returns the output record for [entity], together with its main address.
     */
    fun toRecord(entity: SiteDb): SiteRecord =
        SiteRecord(
            bpn = entity.bpn,
            name = entity.name,
            states = entity.states.map { toStateRecord(it) },
            relations = entity.startSiteRelations.plus(entity.endSiteRelations).map { toRelationRecord(it) },
            legalEntityBpn = entity.legalEntity.bpn,
            confidenceCriteria = toConfidenceCriteriaRecord(entity.confidenceCriteria),
            isDataSpaceParticipant = entity.legalEntity.isDataSpaceParticipant,
            createdAt = entity.createdAt.instant,
            updatedAt = entity.updatedAt.instant,
            mainAddress = toRecord(entity.mainAddress),
            scriptVariants = toSiteScriptVariantRecords(entity)
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: LogisticAddressDb): AddressRecord =
        AddressRecord(
            bpn = entity.bpn,
            legalEntityBpn = entity.legalEntity?.bpn,
            siteBpn = entity.mainSite?.bpn,
            additionalSiteBpns = entity.additionalSites.map { it.bpn },
            createdAt = entity.createdAt.instant,
            updatedAt = entity.updatedAt.instant,
            name = entity.name,
            states = entity.states.map { toStateRecord(it) },
            identifiers = entity.identifiers.map { toIdentifierRecord(it) },
            relations = entity.startAddressRelations.plus(entity.endAddressRelations).map { toRelationRecord(it) },
            physicalPostalAddress = toPhysicalPostalAddressRecord(entity.physicalPostalAddress),
            alternativePostalAddress = entity.alternativePostalAddress?.let { toAlternativePostalAddressRecord(it) },
            confidenceCriteria = toConfidenceCriteriaRecord(entity.confidenceCriteria),
            isDataSpaceParticipant = entity.legalEntity?.isDataSpaceParticipant ?: entity.mainSite?.legalEntity?.isDataSpaceParticipant ?: false,
            addressType = entity.addressType,
            scriptVariants = entity.scriptVariants.map { toScriptVariantRecord(it) }
        )

    /**
     * Returns the business partner search record [entity] represents, together with its legal entity and main site.
     */
    fun toBusinessPartnerSearchRecord(entity: LogisticAddressDb): BusinessPartnerSearchRecord {
        val legalEntity = checkNotNull(entity.legalEntity) { "Business partner search matched address ${entity.bpn} without a legal entity." }

        return BusinessPartnerSearchRecord(
            legalEntity = BusinessPartnerSearchLegalEntityRecord(
                bpn = legalEntity.bpn,
                legalName = legalEntity.legalName.value,
                legalShortName = legalEntity.legalName.shortName,
                legalFormTechnicalKey = legalEntity.legalForm?.technicalKey,
                identifiers = legalEntity.identifiers.map { toIdentifierRecord(it) },
                states = legalEntity.states.map { toStateRecord(it) },
                confidenceCriteria = toConfidenceCriteriaRecord(legalEntity.confidenceCriteria),
                isDataSpaceParticipant = legalEntity.isDataSpaceParticipant
            ),
            site = entity.mainSite?.let { site ->
                BusinessPartnerSearchSiteRecord(
                    bpn = site.bpn,
                    name = site.name,
                    states = site.states.map { toStateRecord(it) },
                    confidenceCriteria = toConfidenceCriteriaRecord(site.confidenceCriteria)
                )
            },
            address = BusinessPartnerSearchAddressRecord(
                bpn = entity.bpn,
                name = entity.name,
                addressType = entity.addressType,
                identifiers = entity.identifiers.map { toIdentifierRecord(it) },
                states = entity.states.map { toStateRecord(it) },
                physicalPostalAddress = toPhysicalPostalAddressRecord(entity.physicalPostalAddress),
                alternativePostalAddress = entity.alternativePostalAddress?.let { toAlternativePostalAddressRecord(it) },
                confidenceCriteria = toConfidenceCriteriaRecord(entity.confidenceCriteria)
            )
        )
    }

    private fun toIdentifierRecord(identifier: LegalEntityIdentifierDb): LegalEntityIdentifierRecord =
        LegalEntityIdentifierRecord(
            value = identifier.value,
            typeTechnicalKey = identifier.type.technicalKey,
            typeName = identifier.type.name,
            issuingBody = identifier.issuingBody
        )

    private fun toIdentifierRecord(identifier: AddressIdentifierDb): AddressIdentifierRecord =
        AddressIdentifierRecord(
            value = identifier.value,
            typeTechnicalKey = identifier.type.technicalKey,
            typeName = identifier.type.name
        )

    private fun toStateRecord(state: LegalEntityStateDb): BusinessStateRecord =
        BusinessStateRecord(validFrom = state.validFrom, validTo = state.validTo, type = state.type)

    private fun toStateRecord(state: SiteStateDb): BusinessStateRecord =
        BusinessStateRecord(validFrom = state.validFrom, validTo = state.validTo, type = state.type)

    private fun toStateRecord(state: AddressStateDb): BusinessStateRecord =
        BusinessStateRecord(validFrom = state.validFrom, validTo = state.validTo, type = state.type)

    private fun toRelationRecord(relation: RelationDb): LegalEntityRelationRecord =
        LegalEntityRelationRecord(
            type = relation.type,
            sourceBpnl = relation.startNode.bpn,
            targetBpnl = relation.endNode.bpn,
            validityPeriods = toValidityPeriodRecords(relation.validityPeriods),
            reasonCode = relation.reasonCode?.technicalKey
        )

    private fun toRelationRecord(relation: SiteRelationDb): SiteRelationRecord =
        SiteRelationRecord(
            type = relation.type,
            sourceBpns = relation.startSite.bpn,
            targetBpns = relation.endSite.bpn,
            validityPeriods = toValidityPeriodRecords(relation.validityPeriods),
            reasonCode = relation.reasonCode?.technicalKey
        )

    private fun toRelationRecord(relation: AddressRelationDb): AddressRelationRecord =
        AddressRelationRecord(
            type = relation.type,
            sourceBpna = relation.startAddress.bpn,
            targetBpna = relation.endAddress.bpn,
            validityPeriods = toValidityPeriodRecords(relation.validityPeriods),
            reasonCode = relation.reasonCode?.technicalKey
        )

    private fun toValidityPeriodRecords(validityPeriods: Collection<RelationValidityPeriodDb>): List<RelationValidityPeriodRecord> =
        validityPeriods.sortedBy { it.validFrom }.map { RelationValidityPeriodRecord(validFrom = it.validFrom, validTo = it.validTo) }

    private fun toConfidenceCriteriaRecord(confidenceCriteria: ConfidenceCriteriaDb): ConfidenceCriteriaRecord =
        ConfidenceCriteriaRecord(
            sharedByOwner = confidenceCriteria.sharedByOwner,
            checkedByExternalDataSource = confidenceCriteria.checkedByExternalDataSource,
            numberOfSharingMembers = confidenceCriteria.numberOfSharingMembers,
            lastConfidenceCheckAt = confidenceCriteria.lastConfidenceCheckAt,
            nextConfidenceCheckAt = confidenceCriteria.nextConfidenceCheckAt,
            confidenceLevel = confidenceCriteria.confidenceLevel
        )

    // The legal address covers every script its legal entity is named in: the parsers reject a variant it does not
    // cover and ScriptVariantCoverageService prunes any the legal address stops covering.
    private fun toLegalEntityScriptVariantRecords(legalEntity: LegalEntityDb): List<LegalEntityScriptVariantRecord> {
        val legalAddressVariantsByCode = legalEntity.legalAddress.scriptVariants.associateBy { it.scriptCode.technicalKey }

        return legalEntity.scriptVariants.map { variant ->
            val scriptCode = variant.scriptCode.technicalKey
            val legalAddressVariant = legalAddressVariantsByCode[scriptCode]
                ?: throw IllegalStateException("Legal entity script variant of script code '$scriptCode' is not covered by the legal address.")
            LegalEntityScriptVariantRecord(
                scriptCode = scriptCode,
                legalName = variant.legalName,
                shortName = variant.shortName,
                legalAddress = toScriptVariantRecord(legalAddressVariant)
            )
        }
    }

    // The main address covers every script its site is named in: the parsers reject a variant it does not cover and
    // ScriptVariantCoverageService prunes any the main address stops covering.
    private fun toSiteScriptVariantRecords(site: SiteDb): List<SiteScriptVariantRecord> {
        val mainAddressVariantsByCode = site.mainAddress.scriptVariants.associateBy { it.scriptCode.technicalKey }

        return site.scriptVariants.map { variant ->
            val scriptCode = variant.scriptCode.technicalKey
            val mainAddressVariant = mainAddressVariantsByCode[scriptCode]
                ?: throw IllegalStateException("Site script variant of script code '$scriptCode' is not covered by the main address.")
            SiteScriptVariantRecord(scriptCode = scriptCode, name = variant.name, mainAddress = toScriptVariantRecord(mainAddressVariant))
        }
    }

    private fun toPhysicalPostalAddressRecord(address: PhysicalPostalAddressDb): PhysicalPostalAddressRecord =
        PhysicalPostalAddressRecord(
            geographicCoordinates = address.geographicCoordinates?.let { toGeoCoordinateRecord(it) },
            country = toRecord(address.country),
            administrativeAreaLevel1 = address.administrativeAreaLevel1?.let { toRecord(it) },
            administrativeAreaLevel2 = address.administrativeAreaLevel2,
            administrativeAreaLevel3 = address.administrativeAreaLevel3,
            postalCode = address.postCode,
            city = address.city,
            district = address.districtLevel1,
            street = address.street?.let { toStreetRecord(it) },
            companyPostalCode = address.companyPostCode,
            industrialZone = address.industrialZone,
            building = address.building,
            floor = address.floor,
            door = address.door,
            taxJurisdictionCode = address.taxJurisdictionCode
        )

    private fun toAlternativePostalAddressRecord(address: AlternativePostalAddressDb): AlternativePostalAddressRecord =
        AlternativePostalAddressRecord(
            geographicCoordinates = address.geographicCoordinates?.let { toGeoCoordinateRecord(it) },
            country = toRecord(address.country),
            administrativeAreaLevel1 = address.administrativeAreaLevel1?.let { toRecord(it) },
            postalCode = address.postCode,
            city = address.city,
            deliveryServiceType = address.deliveryServiceType,
            deliveryServiceNumber = address.deliveryServiceNumber,
            deliveryServiceQualifier = address.deliveryServiceQualifier
        )

    private fun toGeoCoordinateRecord(coordinate: GeographicCoordinateDb): GeoCoordinateRecord =
        GeoCoordinateRecord(longitude = coordinate.longitude, latitude = coordinate.latitude, altitude = coordinate.altitude)

    private fun toStreetRecord(street: StreetDb): StreetRecord =
        StreetRecord(
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

    private fun toScriptVariantRecord(scriptVariant: LogisticAddressScriptVariantDb): AddressScriptVariantRecord =
        AddressScriptVariantRecord(
            scriptCode = scriptVariant.scriptCode.technicalKey,
            name = scriptVariant.name,
            physicalAddress = toPhysicalAddressScriptVariantRecord(scriptVariant.physicalAddress),
            alternativeAddress = scriptVariant.alternativeAddress?.let { AlternativeAddressScriptVariantRecord(city = it.city) }
        )

    private fun toPhysicalAddressScriptVariantRecord(address: PhysicalAddressScriptVariantDb): PhysicalAddressScriptVariantRecord =
        PhysicalAddressScriptVariantRecord(
            city = address.city,
            district = address.district,
            street = address.street?.let { toStreetScriptVariantRecord(it) },
            industrialZone = address.industrialZone,
            building = address.building,
            floor = address.floor,
            door = address.door
        )

    private fun toStreetScriptVariantRecord(street: StreetScriptVariantDb): StreetScriptVariantRecord =
        StreetScriptVariantRecord(
            name = street.name,
            direction = street.direction,
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix
        )
}
