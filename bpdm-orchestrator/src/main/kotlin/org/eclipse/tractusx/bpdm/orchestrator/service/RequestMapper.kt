/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.eclipse.tractusx.bpdm.orchestrator.entity.*
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service

@Service
class RequestMapper {

    fun toBusinessPartner(businessPartner: BusinessPartner) =
        with(businessPartner){
            GoldenRecordTaskDb.BusinessPartner(
                nameParts = toNameParts(businessPartner),
                identifiers = toIdentifiers(businessPartner),
                businessStates = toStates(businessPartner),
                confidences = toConfidences(businessPartner),
                addresses = toPostalAddresses(businessPartner),
                bpnReferences = toBpnReferences(businessPartner),
                legalName = legalEntity.legalName,
                legalShortName = legalEntity.legalShortName,
                siteExists = site != null,
                siteName = site?.siteName,
                legalForm = legalEntity.legalForm,
                isCatenaXMemberData = legalEntity.isParticipantData,
                owningCompany = owningCompany,
                legalEntityHasChanged = legalEntity.hasChanged,
                siteHasChanged = site?.hasChanged
            )
        }

    fun toTaskError(error: TaskErrorDto) =
        with(error) {
            TaskErrorDb(type = type, description = description)
        }

    fun toNameParts(businessPartner: BusinessPartner) =
        mutableListOf(
            businessPartner.uncategorized.nameParts.map { NamePartDb(it, null) },
            businessPartner.nameParts.map { NamePartDb(it.name, it.type) }
        ).flatten().toMutableList()


    fun toIdentifier(identifier: Identifier, scope: IdentifierDb.Scope) =
        with(identifier) {
            IdentifierDb(value, type, issuingBody, scope)
        }


    fun toIdentifiers(businessPartner: BusinessPartner) =
        IdentifierDb.Scope.entries.mapNotNull { scope ->
            when (scope) {
                IdentifierDb.Scope.LegalEntity -> businessPartner.legalEntity.identifiers
                IdentifierDb.Scope.LegalAddress -> businessPartner.legalEntity.legalAddress.identifiers
                IdentifierDb.Scope.SiteMainAddress -> businessPartner.site?.siteMainAddress?.identifiers
                IdentifierDb.Scope.AdditionalAddress -> businessPartner.additionalAddress?.identifiers
                IdentifierDb.Scope.Uncategorized -> businessPartner.uncategorized.identifiers
                IdentifierDb.Scope.UncategorizedAddress -> businessPartner.uncategorized.address?.identifiers
            }?.map { toIdentifier(it, scope) }
        }.flatten().toMutableList()

    fun toState(state: BusinessState, scope: BusinessStateDb.Scope) =
        with(state) {
            BusinessStateDb(validFrom?.toTimestamp(), validTo?.toTimestamp(), type, scope)
        }

    fun toStates(businessPartner: BusinessPartner) =
        BusinessStateDb.Scope.entries.mapNotNull { scope ->
            when (scope) {
                BusinessStateDb.Scope.LegalEntity -> businessPartner.legalEntity.states
                BusinessStateDb.Scope.Site -> businessPartner.site?.states
                BusinessStateDb.Scope.LegalAddress -> businessPartner.legalEntity.legalAddress.states
                BusinessStateDb.Scope.SiteMainAddress -> businessPartner.site?.siteMainAddress?.states
                BusinessStateDb.Scope.AdditionalAddress -> businessPartner.additionalAddress?.states
                BusinessStateDb.Scope.Uncategorized -> businessPartner.uncategorized.states
                BusinessStateDb.Scope.UncategorizedAddress -> businessPartner.uncategorized.address?.states
            }?.map { toState(it, scope) }
        }.flatten().toMutableList()

    fun toConfidence(confidenceCriteria: ConfidenceCriteria) =
        with(confidenceCriteria) {
            ConfidenceCriteriaDb(
                sharedByOwner,
                checkedByExternalDataSource,
                numberOfSharingMembers,
                lastConfidenceCheckAt?.toTimestamp(),
                nextConfidenceCheckAt?.toTimestamp(),
                confidenceLevel
            )
        }

    fun toConfidences(businessPartner: BusinessPartner) =
        ConfidenceCriteriaDb.Scope.entries.mapNotNull { scope ->
            when (scope) {
                ConfidenceCriteriaDb.Scope.LegalEntity -> businessPartner.legalEntity.confidenceCriteria
                ConfidenceCriteriaDb.Scope.Site -> businessPartner.site?.confidenceCriteria
                ConfidenceCriteriaDb.Scope.LegalAddress -> businessPartner.legalEntity.legalAddress.confidenceCriteria
                ConfidenceCriteriaDb.Scope.SiteMainAddress -> businessPartner.site?.siteMainAddress?.confidenceCriteria
                ConfidenceCriteriaDb.Scope.AdditionalAddress -> businessPartner.additionalAddress?.confidenceCriteria
                ConfidenceCriteriaDb.Scope.UncategorizedAddress -> businessPartner.uncategorized.address?.confidenceCriteria
            }?.let { scope to toConfidence(it) }
        }.toMap().toMutableMap()

    fun toPostalAddress(postalAddress: PostalAddress, scope: PostalAddressDb.Scope) =
        with(postalAddress) {
            PostalAddressDb(
                addressName = addressName,
                physicalAddress = toPhysicalAddress(physicalAddress),
                alternativeAddress = toAlternativeAddress(alternativeAddress),
                hasChanged = hasChanged
            )
        }

    fun toPostalAddresses(businessPartner: BusinessPartner) =
        PostalAddressDb.Scope.entries.mapNotNull { scope ->
            when (scope) {
                PostalAddressDb.Scope.LegalAddress -> businessPartner.legalEntity.legalAddress
                PostalAddressDb.Scope.SiteMainAddress -> businessPartner.site?.siteMainAddress
                PostalAddressDb.Scope.AdditionalAddress -> businessPartner.additionalAddress
                PostalAddressDb.Scope.UncategorizedAddress -> businessPartner.uncategorized.address
            }?.let { scope to toPostalAddress(it, scope) }
        }.toMap().toMutableMap()


    fun toBpnReference(bpnReference: BpnReference) =
        with(bpnReference) {
            BpnReferenceDb(
                referenceValue = referenceValue,
                desiredBpn = desiredBpn,
                referenceType = referenceType
            )
        }

    fun toBpnReferences(businessPartner: BusinessPartner) =
        BpnReferenceDb.Scope.entries.mapNotNull { scope ->
            when (scope) {
                BpnReferenceDb.Scope.LegalEntity -> businessPartner.legalEntity.bpnReference
                BpnReferenceDb.Scope.Site -> businessPartner.site?.bpnReference
                BpnReferenceDb.Scope.LegalAddress -> businessPartner.legalEntity.legalAddress.bpnReference
                BpnReferenceDb.Scope.SiteMainAddress -> businessPartner.site?.siteMainAddress?.bpnReference
                BpnReferenceDb.Scope.AdditionalAddress -> businessPartner.additionalAddress?.bpnReference
                BpnReferenceDb.Scope.UncategorizedAddress ->  businessPartner.uncategorized.address?.bpnReference
            }?.let { scope to toBpnReference(it) }
        }.toMap().toMutableMap()

    fun toPhysicalAddress(physicalAddress: PhysicalAddress) =
        with(physicalAddress) {
            PostalAddressDb.PhysicalAddressDb(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = toStreet(street),
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    fun toAlternativeAddress(alternativeAddress: AlternativeAddress?) =
        alternativeAddress?.let {
            with(alternativeAddress) {
                PostalAddressDb.AlternativeAddress(
                    exists = true,
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
        } ?: PostalAddressDb.AlternativeAddress(
            exists = false,
            geographicCoordinates = PostalAddressDb.GeoCoordinate(
                longitude = null,
                latitude = null,
                altitude = null
            ),
            country = null,
            administrativeAreaLevel1 = null,
            postalCode = null,
            city = null,
            deliveryServiceType = null,
            deliveryServiceQualifier = null,
            deliveryServiceNumber = null
        )


    fun toGeoCoordinate(geoCoordinate: GeoCoordinate) =
        with(geoCoordinate) {
            PostalAddressDb.GeoCoordinate(longitude, latitude, altitude)
        }

    fun toStreet(street: Street) =
        with(street) {
            PostalAddressDb.Street(
                name,
                houseNumber,
                houseNumberSupplement,
                milestone,
                direction,
                namePrefix,
                additionalNamePrefix,
                nameSuffix,
                additionalNameSuffix
            )
        }

}