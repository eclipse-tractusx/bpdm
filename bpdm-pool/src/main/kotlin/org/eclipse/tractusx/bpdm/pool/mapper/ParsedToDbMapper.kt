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

import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressContentChangeParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressIdentifierParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressStateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AlternativeAddressScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AlternativePostalAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.GeoCoordinateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.PhysicalAddressScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.PhysicalPostalAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner.ConfidenceCriteriaParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityHeaderChangeParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityIdentifierParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteHeaderChangeParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.address.StreetRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.StreetScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.FieldUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityStateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteStateRaw
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset

/**
 * Maps Parsed models to the entities they persist as and to the change sets that update those entities.
 *
 * Child entities come without their back-reference to the owning business partner unless the owner is passed in; the
 * caller wires it.
 */
@Component
class ParsedToDbMapper {

    /**
     * Returns a new, unsaved address entity for [parsed] under [bpn].
     */
    fun toAddressEntity(bpn: String, parsed: AddressCreateParsed, numberOfSharingMembers: Int): LogisticAddressDb {
        val entity = LogisticAddressDb(
            bpn = bpn,
            legalEntity = parsed.legalEntity,
            name = parsed.address.name,
            physicalPostalAddress = toPhysicalPostalAddress(parsed.address.physicalPostalAddress),
            alternativePostalAddress = parsed.address.alternativePostalAddress?.let { toAlternativePostalAddress(it) },
            confidenceCriteria = toConfidence(parsed.address.confidenceCriteria, numberOfSharingMembers),
            scriptVariants = toAddressScriptVariants(parsed.address.scriptVariants).toMutableList()
        )
        parsed.site?.let { entity.sites.add(it) }
        entity.identifiers.addAll(toAddressIdentifiers(parsed.address.identifiers).onEach { it.address = entity })
        entity.states.addAll(toAddressStates(parsed.address.states).onEach { it.address = entity })
        return entity
    }

    /**
     * Returns a new, unsaved legal entity for [header] under [bpn], with its legal address not yet assigned.
     */
    fun toLegalEntity(bpn: String, header: LegalEntityHeaderParsed, currentness: Instant, numberOfSharingMembers: Int): LegalEntityDb {
        val entity = LegalEntityDb(
            bpn = bpn,
            legalName = NameDb(value = header.legalName, shortName = header.legalShortName),
            legalForm = header.legalForm,
            currentness = currentness,
            confidenceCriteria = toConfidence(header.confidenceCriteria, numberOfSharingMembers),
            isDataSpaceParticipant = header.isDataSpaceParticipant,
            ownershipUltimate = header.ownershipUltimate ?: false,
            scriptVariants = toLegalEntityScriptVariants(header.scriptVariants).toMutableList()
        )
        entity.identifiers.addAll(toLegalEntityIdentifiers(header.identifiers, entity))
        entity.states.addAll(toLegalEntityStates(header.states, entity))
        return entity
    }

    /**
     * Returns a new, unsaved site of [legalEntity] for [header] under [bpn], with its main address not yet assigned.
     */
    fun toSite(bpn: String, legalEntity: LegalEntityDb, header: SiteHeaderParsed, numberOfSharingMembers: Int): SiteDb {
        val entity = SiteDb(
            bpn = bpn,
            name = header.name,
            confidenceCriteria = toConfidence(header.confidenceCriteria, numberOfSharingMembers),
            legalEntity = legalEntity,
            scriptVariants = toSiteScriptVariants(header.scriptVariants).toMutableList()
        )
        entity.states.addAll(toSiteStates(header.states, entity))
        return entity
    }

    /**
     * Returns the physical postal address entity value for [parsed].
     */
    fun toPhysicalPostalAddress(parsed: PhysicalPostalAddressParsed): PhysicalPostalAddressDb =
        PhysicalPostalAddressDb(
            geographicCoordinates = parsed.geographicCoordinates?.let { toGeo(it) },
            country = parsed.country,
            administrativeAreaLevel1 = parsed.administrativeAreaLevel1,
            administrativeAreaLevel2 = parsed.administrativeAreaLevel2,
            administrativeAreaLevel3 = parsed.administrativeAreaLevel3,
            postCode = parsed.postalCode,
            city = parsed.city,
            districtLevel1 = parsed.district,
            street = parsed.street?.let { toStreet(it) },
            companyPostCode = parsed.companyPostalCode,
            industrialZone = parsed.industrialZone,
            building = parsed.building,
            floor = parsed.floor,
            door = parsed.door,
            taxJurisdictionCode = parsed.taxJurisdictionCode
        )

    /**
     * Returns the alternative postal address entity value for [parsed].
     */
    fun toAlternativePostalAddress(parsed: AlternativePostalAddressParsed): AlternativePostalAddressDb =
        AlternativePostalAddressDb(
            geographicCoordinates = parsed.geographicCoordinates?.let { toGeo(it) },
            country = parsed.country,
            administrativeAreaLevel1 = parsed.administrativeAreaLevel1,
            postCode = parsed.postalCode,
            city = parsed.city,
            deliveryServiceType = parsed.deliveryServiceType,
            deliveryServiceNumber = parsed.deliveryServiceNumber,
            deliveryServiceQualifier = parsed.deliveryServiceQualifier
        )

    /**
     * Returns the confidence criteria entity value for [parsed] with the given sharing member count.
     */
    fun toConfidence(parsed: ConfidenceCriteriaParsed, numberOfSharingMembers: Int): ConfidenceCriteriaDb =
        ConfidenceCriteriaDb(
            sharedByOwner = parsed.sharedByOwner,
            checkedByExternalDataSource = parsed.checkedByExternalDataSource,
            numberOfSharingMembers = numberOfSharingMembers,
            lastConfidenceCheckAt = parsed.lastConfidenceCheckAt.toLocalDateTime(),
            nextConfidenceCheckAt = parsed.nextConfidenceCheckAt.toLocalDateTime()
        )

    /**
     * Returns the address identifier entities for [parsed].
     */
    fun toAddressIdentifiers(parsed: List<AddressIdentifierParsed>): List<AddressIdentifierDb> =
        parsed.map { AddressIdentifierDb(value = it.value, type = it.type) }

    /**
     * Returns the address state entities for [parsed].
     */
    fun toAddressStates(parsed: List<AddressStateParsed>): List<AddressStateDb> =
        parsed.map { AddressStateDb(validFrom = it.validFrom?.toLocalDateTime(), validTo = it.validTo?.toLocalDateTime(), type = it.type) }

    /**
     * Returns the address script variant entities for [parsed].
     */
    fun toAddressScriptVariants(parsed: List<AddressScriptVariantParsed>): List<LogisticAddressScriptVariantDb> =
        parsed.map { variant ->
            LogisticAddressScriptVariantDb(
                scriptCode = variant.scriptCode,
                name = variant.address.addressName,
                physicalAddress = toPhysicalScriptVariant(variant.address.physicalAddress),
                alternativeAddress = variant.address.alternativeAddress?.let { toAlternativeScriptVariant(it) }
            )
        }

    /**
     * Returns the identifier entities of [parent] for [parsed].
     */
    fun toLegalEntityIdentifiers(parsed: List<LegalEntityIdentifierParsed>, parent: LegalEntityDb): List<LegalEntityIdentifierDb> =
        parsed.map { LegalEntityIdentifierDb(value = it.value, type = it.type, issuingBody = it.issuingBody, legalEntity = parent) }

    /**
     * Returns the state entities of [parent] for [parsed].
     */
    fun toLegalEntityStates(parsed: List<LegalEntityStateRaw>, parent: LegalEntityDb): List<LegalEntityStateDb> =
        parsed.map { LegalEntityStateDb(validFrom = it.validFrom?.toLocalDateTime(), validTo = it.validTo?.toLocalDateTime(), type = it.type, legalEntity = parent) }

    /**
     * Returns the legal entity script variant entities for [parsed].
     */
    fun toLegalEntityScriptVariants(parsed: List<LegalEntityScriptVariantParsed>): List<LegalEntityScriptVariantDb> =
        parsed.map { LegalEntityScriptVariantDb(scriptCode = it.scriptCode, legalName = it.legalName, shortName = it.shortName) }

    /**
     * Returns the state entities of [parent] for [parsed].
     */
    fun toSiteStates(parsed: List<SiteStateRaw>, parent: SiteDb): List<SiteStateDb> =
        parsed.map { SiteStateDb(validFrom = it.validFrom?.toLocalDateTime(), validTo = it.validTo?.toLocalDateTime(), type = it.type, site = parent) }

    /**
     * Returns the site script variant entities for [parsed].
     */
    fun toSiteScriptVariants(parsed: List<SiteScriptVariantParsed>): List<SiteScriptVariantDb> =
        parsed.map { SiteScriptVariantDb(scriptCode = it.scriptCode, name = it.name) }

    /**
     * Returns the change that replaces an address's whole content with [content] and applies [sites] to its site
     * membership.
     */
    fun toAddressFullChange(content: LogisticAddressParsed, sites: FieldUpdateRaw<List<SiteDb>>) = AddressContentChangeParsed(
        name = FieldUpdateRaw.Set(content.name),
        physicalPostalAddress = FieldUpdateRaw.Set(content.physicalPostalAddress),
        alternativePostalAddress = FieldUpdateRaw.Set(content.alternativePostalAddress),
        confidenceCriteria = FieldUpdateRaw.Set(content.confidenceCriteria),
        identifiers = FieldUpdateRaw.Set(content.identifiers),
        states = FieldUpdateRaw.Set(content.states),
        scriptVariants = FieldUpdateRaw.Set(content.scriptVariants),
        sites = sites
    )

    /**
     * Returns the change that replaces a legal entity's header with [header], leaving the derived ultimate owner and an
     * unstated ownership flag untouched.
     */
    fun toLegalEntityHeaderFullChange(header: LegalEntityHeaderParsed, currentness: Instant) = LegalEntityHeaderChangeParsed(
        legalName = FieldUpdateRaw.Set(header.legalName),
        legalShortName = FieldUpdateRaw.Set(header.legalShortName),
        legalForm = FieldUpdateRaw.Set(header.legalForm),
        confidenceCriteria = FieldUpdateRaw.Set(header.confidenceCriteria),
        isDataSpaceParticipant = FieldUpdateRaw.Set(header.isDataSpaceParticipant),
        // A payload that does not state the ownership flag must not clear it (V6 can never state it).
        ownershipUltimate = header.ownershipUltimate?.let { FieldUpdateRaw.Set(it) } ?: FieldUpdateRaw.NoOp,
        ultimateOwnerBpnl = FieldUpdateRaw.NoOp,
        currentness = FieldUpdateRaw.Set(currentness),
        identifiers = FieldUpdateRaw.Set(header.identifiers),
        states = FieldUpdateRaw.Set(header.states),
        scriptVariants = FieldUpdateRaw.Set(header.scriptVariants)
    )

    /**
     * Returns the change that replaces a site's header with [header].
     */
    fun toSiteHeaderFullChange(header: SiteHeaderParsed) = SiteHeaderChangeParsed(
        name = FieldUpdateRaw.Set(header.name),
        confidenceCriteria = FieldUpdateRaw.Set(header.confidenceCriteria),
        states = FieldUpdateRaw.Set(header.states),
        scriptVariants = FieldUpdateRaw.Set(header.scriptVariants)
    )

    private fun toGeo(geo: GeoCoordinateParsed) =
        GeographicCoordinateDb(latitude = geo.latitude, longitude = geo.longitude, altitude = geo.altitude)

    private fun toStreet(street: StreetRaw) =
        StreetDb(
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

    private fun toPhysicalScriptVariant(variant: PhysicalAddressScriptVariantParsed) =
        PhysicalAddressScriptVariantDb(
            city = variant.city,
            district = variant.district,
            street = variant.street?.let { toScriptVariantStreet(it) },
            industrialZone = variant.industrialZone,
            building = variant.building,
            floor = variant.floor,
            door = variant.door
        )

    private fun toAlternativeScriptVariant(variant: AlternativeAddressScriptVariantParsed) =
        AlternativeAddressScriptVariantDb(
            city = variant.city
        )

    private fun toScriptVariantStreet(street: StreetScriptVariantRaw) =
        StreetScriptVariantDb(
            name = street.name,
            direction = street.direction,
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix
        )

    private fun Instant.toLocalDateTime() = atZone(ZoneOffset.UTC).toLocalDateTime()
}
