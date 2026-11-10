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

package org.eclipse.tractusx.bpdm.orchestrator.mapper.v6

import org.eclipse.tractusx.bpdm.orchestrator.model.request.AlternativeAddressRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BpnReferenceRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BpnReferenceTypeRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BusinessStateRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.ConfidenceCriteriaRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GeoCoordinateRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskCreateRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.IdentifierRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.LegalEntityRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.NamePartRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.NamePartTypeRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PhysicalAddressRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PostalAddressRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PostalAddressWithScriptVariantsRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.SiteRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.StreetRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.UncategorizedPropertiesRequest
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.AlternativeAddressV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.BpnReferenceTypeV6
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.BusinessPartnerV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.BusinessStateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.ConfidenceCriteriaV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.GeoCoordinateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.IdentifierV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.LegalEntityV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.NamePartTypeV6
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.NamePartV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.PhysicalAddressV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.PostalAddressV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.SiteV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.StreetV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskCreateRequestEntryV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.UncategorizedPropertiesV6Dto
import org.springframework.stereotype.Component

/**
 * Translates the fully isolated V6 create-task request entry into the unified [GoldenRecordTaskCreateRequest].
 * V6 now owns its entire DTO graph, so this mapper converts directly to the request model without passing
 * through any V7 DTOs.
 */
@Component
class GoldenRecordTaskCreateInboundMapperV6 {

    fun toRequest(entry: TaskCreateRequestEntryV6Dto): GoldenRecordTaskCreateRequest =
        GoldenRecordTaskCreateRequest(
            recordId = entry.recordId,
            businessPartner = toBusinessPartnerRequest(entry.businessPartner)
        )

    private fun toBusinessPartnerRequest(businessPartner: BusinessPartnerV6Dto): BusinessPartnerRequest =
        with(businessPartner) {
            BusinessPartnerRequest(
                nameParts = nameParts.map(::toNamePartRequest),
                owningCompany = owningCompany,
                uncategorized = toUncategorizedPropertiesRequest(uncategorized),
                legalEntity = toLegalEntityRequest(legalEntity),
                site = site?.let(::toSiteRequest),
                additionalAddress = additionalAddress?.let {
                    PostalAddressWithScriptVariantsRequest(
                        postalProperties = toPostalAddressRequest(it),
                        scriptVariants = emptyList()
                    )
                },
                additionalSites = emptyList()
            )
        }

    private fun toLegalEntityRequest(legalEntity: LegalEntityV6Dto): LegalEntityRequest =
        with(legalEntity) {
            LegalEntityRequest(
                bpnReference = toBpnReferenceRequest(bpnReference),
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = identifiers.map(::toIdentifierRequest),
                states = states.map(::toBusinessStateRequest),
                confidenceCriteria = toConfidenceCriteriaRequest(confidenceCriteria),
                isParticipantData = isCatenaXMemberData,
                hasChanged = hasChanged,
                ownershipUltimate = null,
                ultimateOwnerBpnl = null,
                legalAddress = toPostalAddressRequest(legalAddress),
                scriptVariants = emptyList(),
                goldenRecordRelations = emptyList(),
                updatedAt = null
            )
        }

    private fun toNamePartRequest(namePart: NamePartV6Dto) =
        NamePartRequest(
            name = namePart.name,
            type = when (namePart.type) {
                NamePartTypeV6.LegalName -> NamePartTypeRequest.LegalName
                NamePartTypeV6.ShortName -> NamePartTypeRequest.ShortName
                NamePartTypeV6.LegalForm -> NamePartTypeRequest.LegalForm
                NamePartTypeV6.SiteName -> NamePartTypeRequest.SiteName
                NamePartTypeV6.AddressName -> NamePartTypeRequest.AddressName
            }
        )

    private fun toUncategorizedPropertiesRequest(uncategorized: UncategorizedPropertiesV6Dto) =
        UncategorizedPropertiesRequest(
            nameParts = uncategorized.nameParts,
            identifiers = uncategorized.identifiers.map(::toIdentifierRequest),
            states = uncategorized.states.map(::toBusinessStateRequest),
            address = uncategorized.address?.let {
                PostalAddressWithScriptVariantsRequest(
                    postalProperties = toPostalAddressRequest(it),
                    scriptVariants = emptyList()
                )
            },
            scriptVariants = emptyList()
        )

    private fun toSiteRequest(site: SiteV6Dto) =
        SiteRequest(
            bpnReference = toBpnReferenceRequest(site.bpnReference),
            siteName = site.siteName,
            states = site.states.map(::toBusinessStateRequest),
            confidenceCriteria = toConfidenceCriteriaRequest(site.confidenceCriteria),
            hasChanged = site.hasChanged,
            siteMainAddress = site.siteMainAddress?.let(::toPostalAddressRequest),
            scriptVariants = emptyList(),
            goldenRecordRelations = emptyList(),
            updatedAt = null
        )

    private fun toIdentifierRequest(identifier: IdentifierV6Dto) =
        IdentifierRequest(
            value = identifier.value,
            type = identifier.type,
            issuingBody = identifier.issuingBody
        )

    private fun toBusinessStateRequest(state: BusinessStateV6Dto) =
        BusinessStateRequest(
            validFrom = state.validFrom,
            validTo = state.validTo,
            type = state.type
        )

    private fun toConfidenceCriteriaRequest(confidenceCriteria: ConfidenceCriteriaV6Dto) =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidenceCriteria.sharedByOwner,
            checkedByExternalDataSource = confidenceCriteria.checkedByExternalDataSource,
            numberOfSharingMembers = confidenceCriteria.numberOfSharingMembers,
            lastConfidenceCheckAt = confidenceCriteria.lastConfidenceCheckAt,
            nextConfidenceCheckAt = confidenceCriteria.nextConfidenceCheckAt,
            confidenceLevel = confidenceCriteria.confidenceLevel
        )

    private fun toBpnReferenceRequest(bpnReference: org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.BpnReferenceV6Dto) =
        BpnReferenceRequest(
            referenceValue = bpnReference.referenceValue,
            desiredBpn = bpnReference.desiredBpn,
            referenceType = bpnReference.referenceType?.let {
                when (it) {
                    BpnReferenceTypeV6.Bpn -> BpnReferenceTypeRequest.Bpn
                    BpnReferenceTypeV6.BpnRequestIdentifier -> BpnReferenceTypeRequest.BpnRequestIdentifier
                }
            }
        )

    private fun toPostalAddressRequest(postalAddress: PostalAddressV6Dto) =
        PostalAddressRequest(
            bpnReference = toBpnReferenceRequest(postalAddress.bpnReference),
            addressName = postalAddress.addressName,
            identifiers = postalAddress.identifiers.map(::toIdentifierRequest),
            states = postalAddress.states.map(::toBusinessStateRequest),
            confidenceCriteria = toConfidenceCriteriaRequest(postalAddress.confidenceCriteria),
            physicalAddress = toPhysicalAddressRequest(postalAddress.physicalAddress),
            alternativeAddress = postalAddress.alternativeAddress?.let(::toAlternativeAddressRequest),
            hasChanged = postalAddress.hasChanged,
            goldenRecordRelations = emptyList(),
            updatedAt = null
        )

    private fun toPhysicalAddressRequest(physicalAddress: PhysicalAddressV6Dto) =
        PhysicalAddressRequest(
            geographicCoordinates = toGeoCoordinateRequest(physicalAddress.geographicCoordinates),
            country = physicalAddress.country,
            administrativeAreaLevel1 = physicalAddress.administrativeAreaLevel1,
            administrativeAreaLevel2 = physicalAddress.administrativeAreaLevel2,
            administrativeAreaLevel3 = physicalAddress.administrativeAreaLevel3,
            postalCode = physicalAddress.postalCode,
            city = physicalAddress.city,
            district = physicalAddress.district,
            street = toStreetRequest(physicalAddress.street),
            companyPostalCode = physicalAddress.companyPostalCode,
            industrialZone = physicalAddress.industrialZone,
            building = physicalAddress.building,
            floor = physicalAddress.floor,
            door = physicalAddress.door,
            taxJurisdictionCode = physicalAddress.taxJurisdictionCode
        )

    private fun toAlternativeAddressRequest(alternativeAddress: AlternativeAddressV6Dto) =
        AlternativeAddressRequest(
            geographicCoordinates = toGeoCoordinateRequest(alternativeAddress.geographicCoordinates),
            country = alternativeAddress.country,
            administrativeAreaLevel1 = alternativeAddress.administrativeAreaLevel1,
            postalCode = alternativeAddress.postalCode,
            city = alternativeAddress.city,
            deliveryServiceType = alternativeAddress.deliveryServiceType,
            deliveryServiceQualifier = alternativeAddress.deliveryServiceQualifier,
            deliveryServiceNumber = alternativeAddress.deliveryServiceNumber
        )

    private fun toGeoCoordinateRequest(geoCoordinate: GeoCoordinateV6Dto) =
        GeoCoordinateRequest(
            longitude = geoCoordinate.longitude,
            latitude = geoCoordinate.latitude,
            altitude = geoCoordinate.altitude
        )

    private fun toStreetRequest(street: StreetV6Dto) =
        StreetRequest(
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
}
