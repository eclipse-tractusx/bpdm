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

package org.eclipse.tractusx.bpdm.orchestrator.mapper

import org.eclipse.tractusx.bpdm.orchestrator.model.request.AddressGoldenRecordRelationRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.AddressGoldenRecordRelationTypeRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.AdditionalSiteRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.AlternativeAddressRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.AlternativeAddressScriptVariantRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BusinessStateRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BpnReferenceRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BpnReferenceTypeRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.ConfidenceCriteriaRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GeoCoordinateRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.IdentifierRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.LegalEntityGoldenRecordRelationRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.LegalEntityGoldenRecordRelationTypeRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.LegalEntityRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.LegalEntityScriptVariantRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.NamePartRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.NamePartTypeRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PhysicalAddressRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PhysicalAddressScriptVariantRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PostalAddressRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PostalAddressScriptVariantRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PostalAddressScriptVariantWithScriptCodeRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PostalAddressWithScriptVariantsRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.SiteGoldenRecordRelationRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.SiteGoldenRecordRelationTypeRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.SiteRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.SiteScriptVariantRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.StreetRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.StreetScriptVariantRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.UncategorizedPropertiesRequest
import org.springframework.stereotype.Component
import org.eclipse.tractusx.orchestrator.api.model.AddressGoldenRecordRelation as AddressGoldenRecordRelationDto
import org.eclipse.tractusx.orchestrator.api.model.AdditionalSite as AdditionalSiteDto
import org.eclipse.tractusx.orchestrator.api.model.AlternativeAddress as AlternativeAddressDto
import org.eclipse.tractusx.orchestrator.api.model.AlternativeAddressScriptVariant as AlternativeAddressScriptVariantDto
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner as BusinessPartnerDto
import org.eclipse.tractusx.orchestrator.api.model.BusinessState as BusinessStateDto
import org.eclipse.tractusx.orchestrator.api.model.BpnReference as BpnReferenceDto
import org.eclipse.tractusx.orchestrator.api.model.ConfidenceCriteria as ConfidenceCriteriaDto
import org.eclipse.tractusx.orchestrator.api.model.GeoCoordinate as GeoCoordinateDto
import org.eclipse.tractusx.orchestrator.api.model.Identifier as IdentifierDto
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity as LegalEntityDto
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityGoldenRecordRelation as LegalEntityGoldenRecordRelationDto
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityScriptVariant as LegalEntityScriptVariantDto
import org.eclipse.tractusx.orchestrator.api.model.NamePart as NamePartDto
import org.eclipse.tractusx.orchestrator.api.model.PhysicalAddress as PhysicalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.PhysicalAddressScriptVariant as PhysicalAddressScriptVariantDto
import org.eclipse.tractusx.orchestrator.api.model.PostalAddress as PostalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariant as PostalAddressScriptVariantDto
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariantWithScriptCode as PostalAddressScriptVariantWithScriptCodeDto
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressWithScriptVariants as PostalAddressWithScriptVariantsDto
import org.eclipse.tractusx.orchestrator.api.model.Site as SiteDto
import org.eclipse.tractusx.orchestrator.api.model.SiteGoldenRecordRelation as SiteGoldenRecordRelationDto
import org.eclipse.tractusx.orchestrator.api.model.SiteScriptVariant as SiteScriptVariantDto
import org.eclipse.tractusx.orchestrator.api.model.Street as StreetDto
import org.eclipse.tractusx.orchestrator.api.model.StreetScriptVariant as StreetScriptVariantDto
import org.eclipse.tractusx.orchestrator.api.model.UncategorizedProperties as UncategorizedPropertiesDto
import org.eclipse.tractusx.orchestrator.api.model.NamePartType
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.orchestrator.api.model.SiteGoldenRecordRelationType
import org.eclipse.tractusx.orchestrator.api.model.AddressGoldenRecordRelationType

@Component
class BusinessPartnerRequestMapper {

    fun toBusinessPartnerRequest(businessPartner: BusinessPartnerDto) =
        BusinessPartnerRequest(
            nameParts = businessPartner.nameParts.map(::toNamePartRequest),
            owningCompany = businessPartner.owningCompany,
            uncategorized = toUncategorizedPropertiesRequest(businessPartner.uncategorized),
            legalEntity = toLegalEntityRequest(businessPartner.legalEntity),
            site = businessPartner.site?.let(::toSiteRequest),
            additionalAddress = businessPartner.additionalAddress?.let(::toPostalAddressWithScriptVariantsRequest),
            additionalSites = businessPartner.additionalSites.map(::toAdditionalSiteRequest)
        )

    fun toNamePartRequest(namePart: NamePartDto) =
        NamePartRequest(
            name = namePart.name,
            type = when (namePart.type) {
                NamePartType.LegalName -> NamePartTypeRequest.LegalName
                NamePartType.ShortName -> NamePartTypeRequest.ShortName
                NamePartType.LegalForm -> NamePartTypeRequest.LegalForm
                NamePartType.SiteName -> NamePartTypeRequest.SiteName
                NamePartType.AddressName -> NamePartTypeRequest.AddressName
            }
        )

    fun toIdentifierRequest(identifier: IdentifierDto) =
        IdentifierRequest(
            value = identifier.value,
            type = identifier.type,
            issuingBody = identifier.issuingBody
        )

    fun toBusinessStateRequest(state: BusinessStateDto) =
        BusinessStateRequest(
            validFrom = state.validFrom,
            validTo = state.validTo,
            type = state.type
        )

    fun toBpnReferenceRequest(bpnReference: BpnReferenceDto) =
        BpnReferenceRequest(
            referenceValue = bpnReference.referenceValue,
            desiredBpn = bpnReference.desiredBpn,
            referenceType = bpnReference.referenceType?.let { refType ->
                when (refType) {
                    BpnReferenceType.Bpn -> BpnReferenceTypeRequest.Bpn
                    BpnReferenceType.BpnRequestIdentifier -> BpnReferenceTypeRequest.BpnRequestIdentifier
                }
            }
        )

    fun toPostalAddressRequest(postalAddress: PostalAddressDto) =
        PostalAddressRequest(
            bpnReference = toBpnReferenceRequest(postalAddress.bpnReference),
            addressName = postalAddress.addressName,
            identifiers = postalAddress.identifiers.map(::toIdentifierRequest),
            states = postalAddress.states.map(::toBusinessStateRequest),
            confidenceCriteria = toConfidenceCriteriaRequest(postalAddress.confidenceCriteria),
            physicalAddress = toPhysicalAddressRequest(postalAddress.physicalAddress),
            alternativeAddress = postalAddress.alternativeAddress?.let(::toAlternativeAddressRequest),
            hasChanged = postalAddress.hasChanged,
            goldenRecordRelations = postalAddress.goldenRecordRelations.map(::toAddressGoldenRecordRelationRequest),
            updatedAt = postalAddress.updatedAt
        )

    fun toPhysicalAddressRequest(physicalAddress: PhysicalAddressDto) =
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

    fun toAlternativeAddressRequest(alternativeAddress: AlternativeAddressDto) =
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

    fun toGeoCoordinateRequest(geoCoordinate: GeoCoordinateDto) =
        GeoCoordinateRequest(
            longitude = geoCoordinate.longitude,
            latitude = geoCoordinate.latitude,
            altitude = geoCoordinate.altitude
        )

    fun toStreetRequest(street: StreetDto) =
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

    fun toConfidenceCriteriaRequest(confidenceCriteria: ConfidenceCriteriaDto) =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidenceCriteria.sharedByOwner,
            checkedByExternalDataSource = confidenceCriteria.checkedByExternalDataSource,
            numberOfSharingMembers = confidenceCriteria.numberOfSharingMembers,
            lastConfidenceCheckAt = confidenceCriteria.lastConfidenceCheckAt,
            nextConfidenceCheckAt = confidenceCriteria.nextConfidenceCheckAt,
            confidenceLevel = confidenceCriteria.confidenceLevel
        )

    fun toPostalAddressWithScriptVariantsRequest(address: PostalAddressWithScriptVariantsDto) =
        PostalAddressWithScriptVariantsRequest(
            postalProperties = toPostalAddressRequest(address.postalProperties),
            scriptVariants = address.scriptVariants.map(::toPostalAddressScriptVariantWithScriptCodeRequest)
        )

    fun toLegalEntityRequest(legalEntity: LegalEntityDto) =
        LegalEntityRequest(
            bpnReference = toBpnReferenceRequest(legalEntity.bpnReference),
            legalName = legalEntity.legalName,
            legalShortName = legalEntity.legalShortName,
            legalForm = legalEntity.legalForm,
            identifiers = legalEntity.identifiers.map(::toIdentifierRequest),
            states = legalEntity.states.map(::toBusinessStateRequest),
            confidenceCriteria = toConfidenceCriteriaRequest(legalEntity.confidenceCriteria),
            isParticipantData = legalEntity.isParticipantData,
            hasChanged = legalEntity.hasChanged,
            ownershipUltimate = legalEntity.ownershipUltimate,
            ultimateOwnerBpnl = legalEntity.ultimateOwnerBpnl,
            legalAddress = toPostalAddressRequest(legalEntity.legalAddress),
            scriptVariants = legalEntity.scriptVariants.map(::toLegalEntityScriptVariantRequest),
            goldenRecordRelations = legalEntity.goldenRecordRelations.map(::toLegalEntityGoldenRecordRelationRequest),
            updatedAt = legalEntity.updatedAt
        )

    fun toSiteRequest(site: SiteDto) =
        SiteRequest(
            bpnReference = toBpnReferenceRequest(site.bpnReference),
            siteName = site.siteName,
            states = site.states.map(::toBusinessStateRequest),
            confidenceCriteria = toConfidenceCriteriaRequest(site.confidenceCriteria),
            hasChanged = site.hasChanged,
            siteMainAddress = site.siteMainAddress?.let(::toPostalAddressRequest),
            scriptVariants = site.scriptVariants.map(::toSiteScriptVariantRequest),
            goldenRecordRelations = site.goldenRecordRelations.map(::toSiteGoldenRecordRelationRequest),
            updatedAt = site.updatedAt
        )

    fun toAdditionalSiteRequest(site: AdditionalSiteDto) =
        AdditionalSiteRequest(
            bpnReference = toBpnReferenceRequest(site.bpnReference),
            siteName = site.siteName
        )

    fun toUncategorizedPropertiesRequest(uncategorized: UncategorizedPropertiesDto) =
        UncategorizedPropertiesRequest(
            nameParts = uncategorized.nameParts,
            identifiers = uncategorized.identifiers.map(::toIdentifierRequest),
            states = uncategorized.states.map(::toBusinessStateRequest),
            address = uncategorized.address?.let(::toPostalAddressWithScriptVariantsRequest)
        )

    fun toLegalEntityScriptVariantRequest(scriptVariant: LegalEntityScriptVariantDto) =
        LegalEntityScriptVariantRequest(
            scriptCode = scriptVariant.scriptCode,
            legalName = scriptVariant.legalName,
            legalShortName = scriptVariant.legalShortName,
            legalAddress = toPostalAddressScriptVariantRequest(scriptVariant.legalAddress)
        )

    fun toSiteScriptVariantRequest(scriptVariant: SiteScriptVariantDto) =
        SiteScriptVariantRequest(
            scriptCode = scriptVariant.scriptCode,
            siteName = scriptVariant.siteName,
            mainAddress = toPostalAddressScriptVariantRequest(scriptVariant.mainAddress)
        )

    fun toPostalAddressScriptVariantWithScriptCodeRequest(scriptVariant: PostalAddressScriptVariantWithScriptCodeDto) =
        PostalAddressScriptVariantWithScriptCodeRequest(
            scriptCode = scriptVariant.scriptCode,
            postalProperties = toPostalAddressScriptVariantRequest(scriptVariant.postalProperties)
        )

    fun toPostalAddressScriptVariantRequest(scriptVariant: PostalAddressScriptVariantDto) =
        PostalAddressScriptVariantRequest(
            addressName = scriptVariant.addressName,
            physicalAddress = toPhysicalAddressScriptVariantRequest(scriptVariant.physicalAddress),
            alternativeAddress = scriptVariant.alternativeAddress?.let(::toAlternativeAddressScriptVariantRequest)
        )

    fun toPhysicalAddressScriptVariantRequest(scriptVariant: PhysicalAddressScriptVariantDto) =
        PhysicalAddressScriptVariantRequest(
            city = scriptVariant.city,
            district = scriptVariant.district,
            street = toStreetScriptVariantRequest(scriptVariant.street),
            industrialZone = scriptVariant.industrialZone,
            building = scriptVariant.building,
            floor = scriptVariant.floor,
            door = scriptVariant.door
        )

    fun toAlternativeAddressScriptVariantRequest(scriptVariant: AlternativeAddressScriptVariantDto) =
        AlternativeAddressScriptVariantRequest(
            city = scriptVariant.city
        )

    fun toStreetScriptVariantRequest(scriptVariant: StreetScriptVariantDto) =
        StreetScriptVariantRequest(
            name = scriptVariant.name,
            direction = scriptVariant.direction,
            namePrefix = scriptVariant.namePrefix,
            additionalNamePrefix = scriptVariant.additionalNamePrefix,
            nameSuffix = scriptVariant.nameSuffix,
            additionalNameSuffix = scriptVariant.additionalNameSuffix
        )

    fun toLegalEntityGoldenRecordRelationRequest(relation: LegalEntityGoldenRecordRelationDto) =
        LegalEntityGoldenRecordRelationRequest(
            relationType = when (relation.relationType) {
                LegalEntityGoldenRecordRelationType.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationTypeRequest.IsAlternativeHeadquarterFor
                LegalEntityGoldenRecordRelationType.IsManagedBy -> LegalEntityGoldenRecordRelationTypeRequest.IsManagedBy
                LegalEntityGoldenRecordRelationType.IsOwnedBy -> LegalEntityGoldenRecordRelationTypeRequest.IsOwnedBy
                LegalEntityGoldenRecordRelationType.IsReplacedBy -> LegalEntityGoldenRecordRelationTypeRequest.IsReplacedBy
            },
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    fun toSiteGoldenRecordRelationRequest(relation: SiteGoldenRecordRelationDto) =
        SiteGoldenRecordRelationRequest(
            relationType = when (relation.relationType) {
                SiteGoldenRecordRelationType.IsReplacedBy -> SiteGoldenRecordRelationTypeRequest.IsReplacedBy
            },
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    fun toAddressGoldenRecordRelationRequest(relation: AddressGoldenRecordRelationDto) =
        AddressGoldenRecordRelationRequest(
            relationType = when (relation.relationType) {
                AddressGoldenRecordRelationType.IsReplacedBy -> AddressGoldenRecordRelationTypeRequest.IsReplacedBy
            },
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )
}
