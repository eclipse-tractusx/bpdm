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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.entity.*
import org.springframework.stereotype.Service

@Service
class BusinessPartnerChangesDetector {
    fun haveLegalEntityChanges(original: LegalEntity, updated: LegalEntity): Boolean {
        return (
                original.bpn != updated.bpn ||
                        haveLegalFormChanges(original.legalForm, updated.legalForm) ||
                        haveSetIdentifiersChanges(original.identifiers, updated.identifiers) ||
                        haveSetStateChanges(original.states, updated.states) ||
                        haveSetLogisticAddressChanges(original.addresses, updated.addresses) ||
                        haveSetSiteChanges(original.sites, updated.sites) ||
                        haveSetClassificationChanges(original.classifications, updated.classifications) ||
                        haveLegalNameChanges(original.legalName, updated.legalName) ||
                        haveLogisticAddressChanges(original.legalAddress, updated.legalAddress)
                )
    }


    fun haveSetIdentifiersChanges(originalSet: MutableSet<LegalEntityIdentifier>, updatedSet: MutableSet<LegalEntityIdentifier>): Boolean {

        if (originalSet.size != updatedSet.size) {
            return true
        }

        for (originalEntity in originalSet) {
            val updatedEntity = updatedSet.find { it == originalEntity }

            if (updatedEntity == null || haveIdentifierChanged(originalEntity, updatedEntity)) {
                return true
            }
        }

        return false
    }

    fun haveIdentifierChanged(original: LegalEntityIdentifier, updated: LegalEntityIdentifier): Boolean {
        return (
                original.value != updated.value ||
                        haveIdentifierTypeChanges(original.type, updated.type) ||
                        original.issuingBody != updated.issuingBody
                )
    }

    fun haveIdentifierTypeChanges(original: IdentifierType, updated: IdentifierType): Boolean {
        return (
                original.technicalKey != updated.technicalKey ||
                        original.name != updated.name ||
                        original.businessPartnerType.name != updated.businessPartnerType.name
                )
    }

    fun haveSetStateChanges(
        originalSet: Set<LegalEntityState>,
        updatedSet: Set<LegalEntityState>
    ): Boolean {
        if (originalSet.size != updatedSet.size) {
            return true
        }

        return originalSet.any { originalState ->
            val updatedState = updatedSet.find { it == originalState }
            updatedState == null || haveStateChanges(originalState, updatedState)
        }
    }


    fun haveStateChanges(original: LegalEntityState, updated: LegalEntityState): Boolean {
        return (
                original.description != updated.description ||
                        original.validFrom != updated.validFrom ||
                        original.validTo != updated.validTo ||
                        original.type != updated.type
                )
    }


    fun haveSetClassificationChanges(
        originalSet: Set<LegalEntityClassification>,
        updatedSet: Set<LegalEntityClassification>
    ): Boolean {
        if (originalSet.size != updatedSet.size) {
            return true
        }

        return originalSet.any { originalClassification ->
            val updatedClassification = updatedSet.find { it == originalClassification }
            updatedClassification == null || haveClassificationChanges(originalClassification, updatedClassification)
        }
    }


    fun haveClassificationChanges(original: LegalEntityClassification, updated: LegalEntityClassification): Boolean {
        return (
                original.value != updated.value ||
                        original.code != updated.code ||
                        original.type != updated.type
                )
    }

    fun haveLegalNameChanges(original: Name, updated: Name): Boolean {
        return (
                original.value != updated.value ||
                        original.shortName != updated.shortName
                )
    }

    fun haveLegalFormChanges(original: LegalForm?, updated: LegalForm?): Boolean {
        return (
                original?.name != updated?.name ||
                        original?.technicalKey != updated?.technicalKey ||
                        original?.abbreviation != updated?.abbreviation
                )
    }

    fun haveSetLogisticAddressChanges(originalSet: Set<LogisticAddress>, updatedSet: Set<LogisticAddress>): Boolean {
        if (originalSet.size != updatedSet.size) {
            return true
        }

        return originalSet.any { originalAddress ->
            val updatedAddress = updatedSet.find { it == originalAddress }
            updatedAddress == null || haveLogisticAddressChanges(originalAddress, updatedAddress)
        }
    }


    fun haveLogisticAddressChanges(original: LogisticAddress, updated: LogisticAddress): Boolean {
        return (
                original.bpn != updated.bpn ||
                        original.name != updated.name ||
                        havePhysicalPostalAddressChanges(original.physicalPostalAddress, updated.physicalPostalAddress) ||
                        haveAlternativePostalAddressChanges(original.alternativePostalAddress, updated.alternativePostalAddress)
                )
    }


    fun haveLogisticAddressSiteChanges(original: LogisticAddress, updated: LogisticAddress): Boolean {
        return (
                original.name != updated.name ||
                        havePhysicalPostalAddressChanges(original.physicalPostalAddress, updated.physicalPostalAddress) ||
                        haveAlternativePostalAddressChanges(original.alternativePostalAddress, updated.alternativePostalAddress)
                )
    }


    fun haveSetSiteChanges(originalSet: Set<Site>, updatedSet: Set<Site>): Boolean {
        if (originalSet.size != updatedSet.size) {
            return true
        }

        return originalSet.any { originalSite ->
            val updatedSite = updatedSet.find { it == originalSite }
            updatedSite == null || haveSiteChanges(originalSite, updatedSite)
        }
    }

    fun haveSiteChanges(original: Site, updated: Site): Boolean {
        return (
                original.bpn != updated.bpn ||
                        original.name != updated.name ||
                        haveLogisticAddressSiteChanges(original.mainAddress, updated.mainAddress)
                )
    }

    fun haveAlternativePostalAddressChanges(
        original: AlternativePostalAddress?,
        updated: AlternativePostalAddress?
    ): Boolean {
        return (
                haveGeographicCoordinatesChanges(original?.geographicCoordinates, updated?.geographicCoordinates) ||
                        original?.country != updated?.country ||
                        haveRegionChanges(original?.administrativeAreaLevel1, updated?.administrativeAreaLevel1) ||
                        original?.postCode != updated?.postCode ||
                        original?.city != updated?.city ||
                        original?.deliveryServiceType != updated?.deliveryServiceType ||
                        original?.deliveryServiceNumber != updated?.deliveryServiceNumber ||
                        original?.deliveryServiceQualifier != updated?.deliveryServiceQualifier
                )
    }


    fun haveGeographicCoordinatesChanges(original: GeographicCoordinate?, updated: GeographicCoordinate?): Boolean {
        return (
                original?.latitude != updated?.latitude ||
                        original?.longitude != updated?.longitude ||
                        original?.altitude != updated?.altitude
                )
    }

    fun haveRegionChanges(original: Region?, updated: Region?): Boolean {
        return (
                original?.countryCode != updated?.countryCode ||
                        original?.regionCode != updated?.regionCode ||
                        original?.regionName != updated?.regionName
                )
    }


    fun havePhysicalPostalAddressChanges(
        original: PhysicalPostalAddress,
        updated: PhysicalPostalAddress
    ): Boolean {
        return (
                original.country.name != updated.country.name ||
                        original.administrativeAreaLevel1 != updated.administrativeAreaLevel1 ||
                        original.administrativeAreaLevel2 != updated.administrativeAreaLevel2 ||
                        original.administrativeAreaLevel3 != updated.administrativeAreaLevel3 ||
                        original.administrativeAreaLevel4 != updated.administrativeAreaLevel4 ||
                        original.postCode != updated.postCode ||
                        original.city != updated.city ||
                        original.districtLevel1 != updated.districtLevel1 ||
                        original.districtLevel2 != updated.districtLevel2 ||
                        original.companyPostCode != updated.companyPostCode ||
                        original.industrialZone != updated.industrialZone ||
                        original.building != updated.building ||
                        original.floor != updated.floor ||
                        original.door != updated.door ||
                        haveGeographicCoordinatesChanges(original.geographicCoordinates, updated.geographicCoordinates) ||
                        haveStreetChanges(original.street, updated.street)
                )
    }

    fun haveStreetChanges(original: Street?, updated: Street?): Boolean {

        if (original?.name != updated?.name ||
            original?.houseNumber != updated?.houseNumber ||
            original?.milestone != updated?.milestone ||
            original?.direction != updated?.direction

        ) {
            return true
        }
        return false
    }

}