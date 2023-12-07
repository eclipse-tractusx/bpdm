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
class BusinessPartnerEquivalenceService {
    fun isEquivalent(original: LegalEntity, updated: LegalEntity): Boolean =
        original.bpn != updated.bpn ||
                isEquivalent(original.legalForm, updated.legalForm) ||
                isEquivalentLegalEntityIdentifier(original.identifiers, updated.identifiers) ||
                isEquivalentLegalEntityState(original.states, updated.states) ||
                isEquivalentLogisticAddress(original.addresses, updated.addresses) ||
                isEquivalent(original.sites, updated.sites) ||
                isEquivalentLegalEntityClassification(original.classifications, updated.classifications) ||
                isEquivalent(original.legalName, updated.legalName) ||
                isEquivalent(original.legalAddress, updated.legalAddress)


    private fun isEquivalentLegalEntityIdentifier(
        originalSet: MutableSet<LegalEntityIdentifier>,
        updatedSet: MutableSet<LegalEntityIdentifier>
    ): Boolean {
        return isEquivalent(originalSet, updatedSet) { entity1, entity2 ->
            isEquivalent(entity1, entity2)
        }
    }

    private fun isEquivalent(original: LegalEntityIdentifier, updated: LegalEntityIdentifier): Boolean =
        original.value != updated.value ||
                isEquivalent(original.type, updated.type) ||
                original.issuingBody != updated.issuingBody

    private fun isEquivalent(original: IdentifierType, updated: IdentifierType): Boolean =
        original.technicalKey != updated.technicalKey ||
                original.name != updated.name ||
                original.businessPartnerType.name != updated.businessPartnerType.name


    private fun isEquivalentLegalEntityState(
        originalSet: MutableSet<LegalEntityState>,
        updatedSet: MutableSet<LegalEntityState>
    ): Boolean {
        return isEquivalent(originalSet, updatedSet) { entity1, entity2 ->
            isEquivalent(entity1, entity2)
        }
    }

    private fun isEquivalent(original: LegalEntityState, updated: LegalEntityState): Boolean =
                original.validFrom != updated.validFrom ||
                original.validTo != updated.validTo ||
                original.type != updated.type


    private fun isEquivalentLegalEntityClassification(
        originalSet: MutableSet<LegalEntityClassification>,
        updatedSet: MutableSet<LegalEntityClassification>
    ): Boolean {
        return isEquivalent(originalSet, updatedSet) { entity1, entity2 ->
            isEquivalent(entity1, entity2)
        }
    }


    private fun isEquivalent(original: LegalEntityClassification, updated: LegalEntityClassification): Boolean {
        return (
                original.value != updated.value ||
                        original.code != updated.code ||
                        original.type != updated.type
                )
    }

    private fun isEquivalent(original: Name, updated: Name): Boolean =
        original.value != updated.value ||
                original.shortName != updated.shortName

    private fun isEquivalent(original: LegalForm?, updated: LegalForm?): Boolean =
        original?.name != updated?.name ||
                original?.technicalKey != updated?.technicalKey ||
                original?.abbreviation != updated?.abbreviation


    private fun isEquivalentLogisticAddress(
        originalSet: MutableSet<LogisticAddress>,
        updatedSet: MutableSet<LogisticAddress>
    ): Boolean {
        return isEquivalent(originalSet, updatedSet) { entity1, entity2 ->
            isEquivalent(entity1, entity2)
        }
    }


    fun isEquivalent(original: LogisticAddress, updated: LogisticAddress): Boolean =
        original.bpn != updated.bpn ||
                original.name != updated.name ||
                isEquivalent(original.physicalPostalAddress, updated.physicalPostalAddress) ||
                isEquivalent(original.alternativePostalAddress, updated.alternativePostalAddress)


    fun isEquivalentLogisticAddressSite(original: LogisticAddress, updated: LogisticAddress): Boolean =
        original.name != updated.name ||
                isEquivalent(original.physicalPostalAddress, updated.physicalPostalAddress) ||
                isEquivalent(original.alternativePostalAddress, updated.alternativePostalAddress)


    private fun isEquivalent(
        originalSet: MutableSet<Site>,
        updatedSet: MutableSet<Site>
    ): Boolean {
        return isEquivalent(originalSet, updatedSet) { entity1, entity2 ->
            isEquivalent(entity1, entity2)
        }
    }

    fun isEquivalent(original: Site, updated: Site): Boolean =
        original.bpn != updated.bpn ||
                original.name != updated.name ||
                isEquivalentLogisticAddressSite(original.mainAddress, updated.mainAddress)

    private fun isEquivalent(
        original: AlternativePostalAddress?,
        updated: AlternativePostalAddress?
    ): Boolean =
        isEquivalent(original?.geographicCoordinates, updated?.geographicCoordinates) ||
                original?.country != updated?.country ||
                isEquivalent(original?.administrativeAreaLevel1, updated?.administrativeAreaLevel1) ||
                original?.postCode != updated?.postCode ||
                original?.city != updated?.city ||
                original?.deliveryServiceType != updated?.deliveryServiceType ||
                original?.deliveryServiceNumber != updated?.deliveryServiceNumber ||
                original?.deliveryServiceQualifier != updated?.deliveryServiceQualifier


    private fun isEquivalent(original: GeographicCoordinate?, updated: GeographicCoordinate?): Boolean =
        original?.latitude != updated?.latitude ||
                original?.longitude != updated?.longitude ||
                original?.altitude != updated?.altitude

    private fun isEquivalent(original: Region?, updated: Region?): Boolean =
        original?.countryCode != updated?.countryCode ||
                original?.regionCode != updated?.regionCode ||
                original?.regionName != updated?.regionName


    private fun isEquivalent(
        original: PhysicalPostalAddress,
        updated: PhysicalPostalAddress
    ): Boolean =
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
                isEquivalent(original.geographicCoordinates, updated.geographicCoordinates) ||
                isEquivalent(original.street, updated.street)

    private fun isEquivalent(original: Street?, updated: Street?): Boolean =
        original?.name != updated?.name ||
                original?.houseNumber != updated?.houseNumber ||
                original?.milestone != updated?.milestone ||
                original?.direction != updated?.direction

    private fun <T> isEquivalent(
        originalSet: MutableSet<T>,
        updatedSet: MutableSet<T>,
        comparator: (T, T) -> Boolean
    ): Boolean {
        if (originalSet.size != updatedSet.size) {
            return true
        }

        for (originalEntity in originalSet) {
            val updatedEntity = updatedSet.find { comparator(it, originalEntity) }

            if (updatedEntity == null || comparator(originalEntity, updatedEntity)) {
                return true
            }
        }

        return false
    }
}