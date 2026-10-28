/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCandidate
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputContent
import org.eclipse.tractusx.bpdm.gate.model.value.BusinessPartnerValues
import org.eclipse.tractusx.orchestrator.api.model.AdditionalSite
import org.eclipse.tractusx.orchestrator.api.model.AlternativeAddress
import org.eclipse.tractusx.orchestrator.api.model.AlternativeAddressScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.BpnReference
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.model.BusinessState
import org.eclipse.tractusx.orchestrator.api.model.ConfidenceCriteria
import org.eclipse.tractusx.orchestrator.api.model.GeoCoordinate
import org.eclipse.tractusx.orchestrator.api.model.Identifier
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.PhysicalAddress
import org.eclipse.tractusx.orchestrator.api.model.PhysicalAddressScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.PostalAddress
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariantWithScriptCode
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressWithScriptVariants
import org.eclipse.tractusx.orchestrator.api.model.Site
import org.eclipse.tractusx.orchestrator.api.model.SiteScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.Street
import org.eclipse.tractusx.orchestrator.api.model.StreetScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequestEntry
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskResultStateSearchRequest
import org.eclipse.tractusx.orchestrator.api.model.UncategorizedProperties
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import org.eclipse.tractusx.bpdm.gate.model.value.GeoCoordinate as GateGeoCoordinate
import org.eclipse.tractusx.bpdm.gate.model.value.Street as GateStreet

/**
 * Maps business partner golden record task requests onto the requests the Orchestrator receives.
 */
@Component
class GoldenRecordTaskRequestMapper {

    /**
     * Maps [candidates] onto one request creating a golden record task from the sharing member side for each of them.
     */
    fun toCreateRequest(candidates: List<GoldenRecordTaskCandidate>): TaskCreateRequest =
        TaskCreateRequest(
            mode = TaskMode.UpdateFromSharingMember,
            requests = candidates.map { TaskCreateRequestEntry(recordId = it.recordId, businessPartner = toBusinessPartner(it)) }
        )

    /**
     * Maps [taskIds] onto a request asking for the result state of each of these tasks.
     */
    fun toResultStateSearchRequest(taskIds: List<String>): TaskResultStateSearchRequest =
        TaskResultStateSearchRequest(taskIds)

    private fun toBusinessPartner(candidate: GoldenRecordTaskCandidate): BusinessPartner {
        val input = candidate.input
        val addressType = input.address.addressType
        val postalAddress = toPostalAddress(input)
        val isLegalAddress = addressType == AddressType.LegalAddress || addressType == AddressType.LegalAndSiteMainAddress
        val isSiteMainAddress = addressType == AddressType.SiteMainAddress

        return BusinessPartner(
            nameParts = emptyList(),
            uncategorized = UncategorizedProperties(
                nameParts = input.nameParts,
                // The Gate stores every input identifier as uncategorized, so none belongs to the legal entity or address.
                identifiers = input.identifiers.map { Identifier(it.value, it.type, it.issuingBody) },
                states = toStates(input, BusinessPartnerType.GENERIC),
                address = postalAddress.takeIf { addressType == null }?.let { toWithScriptVariants(it, input) }
            ),
            owningCompany = candidate.owningCompanyBpnl,
            legalEntity = LegalEntity(
                bpnReference = toBpnReference(input.legalEntity.bpn),
                legalName = input.legalEntity.legalName,
                legalForm = input.legalEntity.legalForm,
                legalShortName = input.legalEntity.shortName,
                identifiers = emptyList(),
                states = toStates(input, BusinessPartnerType.LEGAL_ENTITY),
                confidenceCriteria = ConfidenceCriteria.empty,
                isParticipantData = null,
                hasChanged = true,
                ownershipUltimate = input.legalEntity.ownershipUltimate,
                ultimateOwnerBpnl = input.legalEntity.ultimateOwnerBpnl,
                legalAddress = postalAddress.takeIf { isLegalAddress } ?: PostalAddress.empty,
                scriptVariants = if (isLegalAddress) input.scriptVariants.map(::toLegalEntityScriptVariant) else emptyList()
            ),
            site = Site(
                bpnReference = toBpnReference(input.site.bpn),
                siteName = input.site.name,
                states = toStates(input, BusinessPartnerType.SITE),
                confidenceCriteria = ConfidenceCriteria.empty,
                hasChanged = true,
                siteMainAddress = postalAddress.takeIf { isSiteMainAddress },
                scriptVariants = if (isSiteMainAddress) input.scriptVariants.map(::toSiteScriptVariant) else emptyList()
            ).takeIf { input.site.bpn != null || input.site.name != null },
            additionalAddress = postalAddress.takeIf { addressType == AddressType.AdditionalAddress }?.let { toWithScriptVariants(it, input) },
            additionalSites = input.additionalSites.map { AdditionalSite(bpnReference = toBpnReference(it.bpn), siteName = it.name) }
        )
    }

    private fun toPostalAddress(input: BusinessPartnerInputContent): PostalAddress =
        PostalAddress(
            bpnReference = toBpnReference(input.address.bpn),
            addressName = input.address.name,
            identifiers = emptyList(),
            states = toStates(input, BusinessPartnerType.ADDRESS),
            confidenceCriteria = ConfidenceCriteria.empty,
            physicalAddress = input.address.physicalPostalAddress?.let(::toPhysicalAddress) ?: PhysicalAddress.empty,
            alternativeAddress = input.address.alternativePostalAddress?.let(::toAlternativeAddress),
            hasChanged = null
        )

    private fun toWithScriptVariants(postalAddress: PostalAddress, input: BusinessPartnerInputContent) =
        PostalAddressWithScriptVariants(
            postalAddress,
            input.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, toPostalAddressScriptVariant(it)) }
        )

    private fun toPhysicalAddress(address: BusinessPartnerValues.PhysicalPostalAddress) =
        with(address) {
            PhysicalAddress(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = street?.let(::toStreet) ?: Street.empty,
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    private fun toAlternativeAddress(address: BusinessPartnerValues.AlternativePostalAddress) =
        with(address) {
            AlternativeAddress(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                postalCode = postalCode,
                city = city,
                deliveryServiceType = deliveryServiceType,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }

    private fun toStreet(street: GateStreet) =
        with(street) {
            Street(
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
        }

    private fun toGeoCoordinate(coordinate: GateGeoCoordinate?) =
        coordinate?.let { GeoCoordinate(it.longitude, it.latitude, it.altitude) } ?: GeoCoordinate.empty

    private fun toStates(input: BusinessPartnerInputContent, businessPartnerType: BusinessPartnerType) =
        input.states.filter { it.businessPartnerType == businessPartnerType }.map {
            BusinessState(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type)
        }

    private fun toBpnReference(bpn: String?) =
        bpn?.let { BpnReference(referenceValue = it, desiredBpn = null, referenceType = BpnReferenceType.Bpn) } ?: BpnReference.empty

    private fun toLegalEntityScriptVariant(scriptVariant: BusinessPartnerValues.ScriptVariant) =
        LegalEntityScriptVariant(scriptVariant.scriptCode, scriptVariant.legalName, scriptVariant.shortName, toPostalAddressScriptVariant(scriptVariant))

    private fun toSiteScriptVariant(scriptVariant: BusinessPartnerValues.ScriptVariant) =
        SiteScriptVariant(scriptVariant.scriptCode, scriptVariant.siteName ?: "", toPostalAddressScriptVariant(scriptVariant))

    private fun toPostalAddressScriptVariant(scriptVariant: BusinessPartnerValues.ScriptVariant) =
        PostalAddressScriptVariant(
            addressName = scriptVariant.addressName,
            physicalAddress = toPhysicalAddressScriptVariant(scriptVariant.physicalAddress),
            alternativeAddress = scriptVariant.alternativeAddressCity?.let { AlternativeAddressScriptVariant(city = it) }
        )

    private fun toPhysicalAddressScriptVariant(address: BusinessPartnerValues.PhysicalAddressScriptVariant) =
        with(address) {
            PhysicalAddressScriptVariant(
                city = city,
                district = district,
                street = with(street) { StreetScriptVariant(name, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix) },
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }
}
