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

package org.eclipse.tractusx.bpdm.pool.mapper.apiv6

import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressIdentifierRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressStateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AlternativePostalAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.LogisticAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressIdentifierV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AlternativePostalAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.PhysicalPostalAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.StreetV6Dto
import org.eclipse.tractusx.bpdm.pool.model.raw.address.StreetRaw
import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.BpnRequestIdentifierSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.IdentifiersSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.businesspartner.ConfidenceCriteriaV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.changelog.ChangelogSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntitySearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierTypeV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.LegalFormV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.participant.CxMembershipSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.participant.CxMembershipUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteCreateWithLegalAddressAsMainV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteV6Dto
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityStateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteStateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressGetRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.LegalEntityAddressSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.bpn.BpnIdentifierSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.bpn.BpnRequestIdentifierSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.changelog.ChangelogSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityGetRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntitySearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.FieldQualityRuleSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeDetailRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.LegalFormCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.participation.DataSpaceParticipantSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.participation.DataSpaceParticipantUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.LegalEntitySiteSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteCreateWithLegalAddressAsMainRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteGetRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityContentRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityHeaderRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityIdentifierRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteContentRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteHeaderRaw
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Maps API v6 requests to their raw model.
 */
@Component
class ApiV6InboundMapper {

    /**
     * Returns the raw identifier type search criteria a client sent.
     */
    fun toIdentifierTypeSearchRaw(businessPartnerType: IdentifierBusinessPartnerTypeV6, country: String?): IdentifierTypeSearchRaw =
        IdentifierTypeSearchRaw(
            businessPartnerType = toModel(businessPartnerType),
            country = country
        )

    /**
     * Returns the raw field quality rule search for [country].
     */
    fun toFieldQualityRuleSearchRaw(country: String): FieldQualityRuleSearchRaw =
        FieldQualityRuleSearchRaw(country = country)

    /**
     * Returns the raw identifier type create holding [identifierType].
     */
    fun toIdentifierTypeCreateRaw(identifierType: IdentifierTypeV6Dto): IdentifierTypeCreateRaw =
        IdentifierTypeCreateRaw(
            technicalKey = identifierType.technicalKey,
            businessPartnerType = toModel(identifierType.businessPartnerType),
            name = identifierType.name,
            abbreviation = identifierType.abbreviation,
            transliteratedName = identifierType.transliteratedName,
            transliteratedAbbreviation = identifierType.transliteratedAbbreviation,
            format = null,
            categories = emptySet(),
            details = identifierType.details.map { IdentifierTypeDetailRaw(it.country, it.mandatory) }
        )

    /**
     * Returns the raw legal form create holding [legalForm].
     */
    fun toLegalFormCreateRaw(legalForm: LegalFormV6Request): LegalFormCreateRaw =
        LegalFormCreateRaw(
            technicalKey = legalForm.technicalKey,
            name = legalForm.name,
            transliteratedName = legalForm.transliteratedName,
            abbreviations = legalForm.abbreviation,
            transliteratedAbbreviations = legalForm.transliteratedAbbreviations,
            country = legalForm.country,
            language = legalForm.language,
            administrativeAreaLevel1 = legalForm.administrativeAreaLevel1,
            isActive = legalForm.isActive
        )

    /**
     * Returns the raw BPN-by-identifier search holding [request].
     */
    fun toBpnIdentifierSearchRaw(request: IdentifiersSearchV6Request): BpnIdentifierSearchRaw =
        BpnIdentifierSearchRaw(
            businessPartnerType = toModel(request.businessPartnerType),
            identifierTypeKey = request.idType,
            identifierValues = request.idValues
        )

    /**
     * Returns the raw BPN-by-request-identifier search holding [request].
     */
    fun toBpnRequestIdentifierSearchRaw(request: BpnRequestIdentifierSearchV6Request): BpnRequestIdentifierSearchRaw =
        BpnRequestIdentifierSearchRaw(requestedIdentifiers = request.requestedIdentifiers)

    /**
     * Returns the raw changelog search holding [request].
     */
    fun toChangelogSearchRaw(request: ChangelogSearchV6Request): ChangelogSearchRaw =
        ChangelogSearchRaw(
            bpns = request.bpns,
            businessPartnerTypes = request.businessPartnerTypes,
            timestampAfter = request.timestampAfter
        )

    /**
     * Returns the raw data space participant search holding [request].
     */
    fun toDataSpaceParticipantSearchRaw(request: CxMembershipSearchV6Request): DataSpaceParticipantSearchRaw =
        DataSpaceParticipantSearchRaw(legalEntityBpns = request.bpnLs, isDataSpaceParticipant = request.isCatenaXMember)

    /**
     * Returns one raw participation update per membership in [request], in the order they were sent.
     */
    fun toDataSpaceParticipantUpdateRaws(request: CxMembershipUpdateV6Request): List<DataSpaceParticipantUpdateRaw> =
        request.memberships.map {
            DataSpaceParticipantUpdateRaw(legalEntityBpn = it.bpnL, isDataSpaceParticipant = it.isCatenaXMember)
        }

    /**
     * Returns the raw legal entity fetch for the legal entity carrying [identifierValue] of [identifierType].
     */
    fun toLegalEntityGetRaw(identifierValue: String, identifierType: String?): LegalEntityGetRaw =
        LegalEntityGetRaw(identifierValue = identifierValue, identifierType = identifierType)

    /**
     * Returns the raw legal entity search holding [request].
     */
    fun toLegalEntitySearchRaw(request: LegalEntitySearchV6Request): LegalEntitySearchRaw =
        LegalEntitySearchRaw(legalEntityBpns = request.bpnLs, legalName = request.legalName)

    /**
     * Returns the raw create for the legal entity and legal address a client sent.
     */
    fun toLegalEntityCreateRaw(request: LegalEntityPartnerCreateV6Request): LegalEntityCreateRaw =
        LegalEntityCreateRaw(content = toContentRequest(request.legalEntity, request.legalAddress))

    /**
     * Returns the raw update for the legal entity and legal address a client sent, addressed by its BPN.
     */
    fun toLegalEntityUpdateRaw(request: LegalEntityPartnerUpdateV6Request): LegalEntityUpdateRaw =
        LegalEntityUpdateRaw(legalEntityBpn = request.bpnl, content = toContentRequest(request.legalEntity, request.legalAddress))

    /**
     * Returns the raw site fetch for the site carrying [bpn].
     */
    fun toSiteGetRaw(bpn: String): SiteGetRaw =
        SiteGetRaw(bpn = bpn)

    /**
     * Returns the raw address fetch for the address carrying [bpn].
     */
    fun toAddressGetRaw(bpn: String): AddressGetRaw =
        AddressGetRaw(bpn = bpn)

    /**
     * Returns the raw site search holding [request].
     */
    fun toSiteSearchRaw(request: SiteSearchV6Request): SiteSearchRaw =
        SiteSearchRaw(siteBpns = request.siteBpns, legalEntityBpns = request.legalEntityBpns, name = request.name)

    /**
     * Returns the raw search for the sites of the legal entity carrying [legalEntityBpn].
     */
    fun toLegalEntitySiteSearchRaw(legalEntityBpn: String): LegalEntitySiteSearchRaw =
        LegalEntitySiteSearchRaw(legalEntityBpn = legalEntityBpn)

    /**
     * Returns the raw address search holding [request].
     */
    fun toAddressSearchRaw(request: AddressSearchV6Request): AddressSearchRaw =
        AddressSearchRaw(
            addressBpns = request.addressBpns,
            siteBpns = request.siteBpns,
            legalEntityBpns = request.legalEntityBpns,
            name = request.name
        )

    /**
     * Returns the raw search for the addresses of the legal entity carrying [legalEntityBpn].
     */
    fun toLegalEntityAddressSearchRaw(legalEntityBpn: String): LegalEntityAddressSearchRaw =
        LegalEntityAddressSearchRaw(legalEntityBpn = legalEntityBpn)

    /**
     * Returns the raw create for the address a client sent, under the parent legal entity or site it named.
     */
    fun toAddressCreateRaw(request: AddressPartnerCreateV6Request): AddressCreateRaw =
        AddressCreateRaw(parentBpn = request.bpnParent, content = toAddressContentRaw(request.address))

    /**
     * Returns the raw update for the address a client sent, addressed by its BPN and leaving its site membership as it is.
     */
    fun toAddressUpdateRaw(request: AddressPartnerUpdateV6Request): AddressUpdateRaw =
        AddressUpdateRaw(addressBpn = request.bpna, siteBpns = null, content = toAddressContentRaw(request.address))

    /**
     * Returns the raw create for the site and main address a client sent, under the parent legal entity it named.
     */
    fun toSiteCreateRaw(request: SitePartnerCreateV6Request): SiteCreateRaw =
        SiteCreateRaw(legalEntityBpn = request.bpnlParent, content = toContentRequest(request.site))

    /**
     * Returns the raw create for the site a client sent that takes its parent's legal address as main address.
     */
    fun toSiteCreateWithLegalAddressAsMainRaw(request: SiteCreateWithLegalAddressAsMainV6Request): SiteCreateWithLegalAddressAsMainRaw =
        SiteCreateWithLegalAddressAsMainRaw(
            legalEntityBpn = request.bpnLParent,
            header = SiteHeaderRaw(
                name = request.name,
                states = request.states.map { toStateRequest(it) },
                confidenceCriteria = toConfidenceRequest(request.confidenceCriteria),
                scriptVariants = emptyList()
            ),
            // V6 lets a legal address be the main address of one site at most; V7 dropped that rule.
            exclusiveLegalAddress = true
        )

    /**
     * Returns the raw update for the site and main address a client sent, addressed by the site's BPN.
     */
    fun toSiteUpdateRaw(request: SitePartnerUpdateV6Request): SiteUpdateRaw =
        SiteUpdateRaw(siteBpn = request.bpns, content = toContentRequest(request.site))

    private fun toContentRequest(site: SiteV6Dto): SiteContentRaw =
        SiteContentRaw(
            header = toHeaderRequest(site),
            mainAddress = toAddressContentRaw(site.mainAddress)
        )

    private fun toContentRequest(legalEntity: LegalEntityV6Dto, legalAddress: LogisticAddressV6Dto): LegalEntityContentRaw =
        LegalEntityContentRaw(
            header = toHeaderRequest(legalEntity),
            legalAddress = toAddressContentRaw(legalAddress)
        )

    private fun toHeaderRequest(legalEntity: LegalEntityV6Dto): LegalEntityHeaderRaw =
        LegalEntityHeaderRaw(
            legalName = legalEntity.legalName,
            legalShortName = legalEntity.legalShortName,
            legalForm = legalEntity.legalForm,
            identifiers = legalEntity.identifiers.map { LegalEntityIdentifierRaw(it.value, it.type, it.issuingBody) },
            states = legalEntity.states.map { toStateRequest(it) },
            confidenceCriteria = toConfidenceRequest(legalEntity.confidenceCriteria),
            isDataSpaceParticipant = legalEntity.isCatenaXMemberData,
            // V6 has no ownership flag, so a V6 write must never change it.
            ownershipUltimate = null,
            scriptVariants = emptyList()
        )

    private fun toStateRequest(state: LegalEntityStateV6Dto): LegalEntityStateRaw =
        LegalEntityStateRaw(state.validFrom.toValidFromInstant(), state.validTo.toValidToInstant(), state.type)

    private fun toHeaderRequest(site: SiteV6Dto): SiteHeaderRaw =
        SiteHeaderRaw(
            name = site.name,
            states = site.states.map { toStateRequest(it) },
            confidenceCriteria = toConfidenceRequest(site.confidenceCriteria),
            scriptVariants = emptyList()
        )

    private fun toStateRequest(state: SiteStateV6Dto): SiteStateRaw =
        SiteStateRaw(state.validFrom.toValidFromInstant(), state.validTo.toValidToInstant(), state.type)

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaV6Dto): ConfidenceCriteriaRaw =
        ConfidenceCriteriaRaw(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant()
        )

    private fun toAddressContentRaw(address: LogisticAddressV6Dto): LogisticAddressRaw =
        LogisticAddressRaw(
            name = address.name,
            states = address.states.map { toStateRequest(it) },
            identifiers = address.identifiers.map { toIdentifierRequest(it) },
            physicalPostalAddress = toPhysicalRequest(address.physicalPostalAddress),
            alternativePostalAddress = address.alternativePostalAddress?.let { toAlternativeRequest(it) },
            confidenceCriteria = toConfidenceRequest(address.confidenceCriteria),
            // v6 has no script variants and an upsert replaces the full list, so a v6 write drops the script
            // variants the business partner may have gained through v7 or the task path.
            scriptVariants = emptyList()
        )

    private fun toPhysicalRequest(physical: PhysicalPostalAddressV6Dto): PhysicalPostalAddressRaw =
        PhysicalPostalAddressRaw(
            geographicCoordinates = physical.geographicCoordinates?.let { toGeoRequest(it) },
            country = physical.country,
            administrativeAreaLevel1 = physical.administrativeAreaLevel1,
            administrativeAreaLevel2 = physical.administrativeAreaLevel2,
            administrativeAreaLevel3 = physical.administrativeAreaLevel3,
            postalCode = physical.postalCode,
            city = physical.city,
            district = physical.district,
            street = physical.street?.let { toStreet(it) },
            companyPostalCode = physical.companyPostalCode,
            industrialZone = physical.industrialZone,
            building = physical.building,
            floor = physical.floor,
            door = physical.door,
            taxJurisdictionCode = physical.taxJurisdictionCode
        )

    private fun toAlternativeRequest(alternative: AlternativePostalAddressV6Dto): AlternativePostalAddressRaw =
        AlternativePostalAddressRaw(
            geographicCoordinates = alternative.geographicCoordinates?.let { toGeoRequest(it) },
            country = alternative.country,
            administrativeAreaLevel1 = alternative.administrativeAreaLevel1,
            postalCode = alternative.postalCode,
            city = alternative.city,
            deliveryServiceType = alternative.deliveryServiceType,
            deliveryServiceQualifier = alternative.deliveryServiceQualifier,
            deliveryServiceNumber = alternative.deliveryServiceNumber
        )

    private fun toStateRequest(state: AddressStateV6Dto): AddressStateRaw =
        AddressStateRaw(validFrom = state.validFrom.toValidFromInstant(), validTo = state.validTo.toValidToInstant(), type = state.type)

    private fun toIdentifierRequest(identifier: AddressIdentifierV6Dto): AddressIdentifierRaw =
        AddressIdentifierRaw(value = identifier.value, type = identifier.type)

    private fun toGeoRequest(geo: GeoCoordinateDto): GeoCoordinateRaw =
        GeoCoordinateRaw(longitude = geo.longitude, latitude = geo.latitude, altitude = geo.altitude)

    private fun toStreet(street: StreetV6Dto): StreetRaw =
        StreetRaw(
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

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)

    private fun LocalDateTime?.toValidFromInstant() = BusinessStateDates.receiveValidFrom(this)?.toUtcInstant()

    private fun LocalDateTime?.toValidToInstant() = BusinessStateDates.receiveValidTo(this)?.toUtcInstant()

    private fun toModel(businessPartnerType: IdentifierBusinessPartnerTypeV6): IdentifierBusinessPartnerType =
        when (businessPartnerType) {
            IdentifierBusinessPartnerTypeV6.LEGAL_ENTITY -> IdentifierBusinessPartnerType.LEGAL_ENTITY
            IdentifierBusinessPartnerTypeV6.ADDRESS -> IdentifierBusinessPartnerType.ADDRESS
        }
}
