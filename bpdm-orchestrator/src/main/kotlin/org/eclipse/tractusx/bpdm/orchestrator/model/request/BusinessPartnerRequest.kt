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

package org.eclipse.tractusx.bpdm.orchestrator.model.request

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import java.time.Instant

data class BusinessPartnerRequest(
    val nameParts: List<NamePartRequest>,
    val owningCompany: String?,
    val uncategorized: UncategorizedPropertiesRequest,
    val legalEntity: LegalEntityRequest,
    val site: SiteRequest?,
    val additionalAddress: PostalAddressWithScriptVariantsRequest?,
    val additionalSites: List<AdditionalSiteRequest> = emptyList()
)

data class NamePartRequest(
    val name: String,
    val type: NamePartTypeRequest
)

data class IdentifierRequest(
    val value: String?,
    val type: String?,
    val issuingBody: String?
)

data class BusinessStateRequest(
    val validFrom: Instant?,
    val validTo: Instant?,
    val type: BusinessStateType?
)

data class BpnReferenceRequest(
    val referenceValue: String?,
    val desiredBpn: String?,
    val referenceType: BpnReferenceTypeRequest?
)

data class PostalAddressRequest(
    val bpnReference: BpnReferenceRequest,
    val addressName: String?,
    val identifiers: List<IdentifierRequest>,
    val states: List<BusinessStateRequest>,
    val confidenceCriteria: ConfidenceCriteriaRequest,
    val physicalAddress: PhysicalAddressRequest,
    val alternativeAddress: AlternativeAddressRequest?,
    val hasChanged: Boolean?,
    val goldenRecordRelations: List<AddressGoldenRecordRelationRequest> = emptyList(),
    val updatedAt: Instant? = null
)

data class PhysicalAddressRequest(
    val geographicCoordinates: GeoCoordinateRequest,
    val country: String?,
    val administrativeAreaLevel1: String?,
    val administrativeAreaLevel2: String?,
    val administrativeAreaLevel3: String?,
    val postalCode: String?,
    val city: String?,
    val district: String?,
    val street: StreetRequest,
    val companyPostalCode: String?,
    val industrialZone: String?,
    val building: String?,
    val floor: String?,
    val door: String?,
    val taxJurisdictionCode: String?
)

data class AlternativeAddressRequest(
    val geographicCoordinates: GeoCoordinateRequest,
    val country: String?,
    val administrativeAreaLevel1: String?,
    val postalCode: String?,
    val city: String?,
    val deliveryServiceType: DeliveryServiceType?,
    val deliveryServiceQualifier: String?,
    val deliveryServiceNumber: String?
)

data class GeoCoordinateRequest(
    val longitude: Double?,
    val latitude: Double?,
    val altitude: Double?
)

data class StreetRequest(
    val name: String?,
    val houseNumber: String?,
    val houseNumberSupplement: String?,
    val milestone: String?,
    val direction: String?,
    val namePrefix: String?,
    val additionalNamePrefix: String?,
    val nameSuffix: String?,
    val additionalNameSuffix: String?
)

data class ConfidenceCriteriaRequest(
    val sharedByOwner: Boolean?,
    val checkedByExternalDataSource: Boolean?,
    val numberOfSharingMembers: Int?,
    val lastConfidenceCheckAt: Instant?,
    val nextConfidenceCheckAt: Instant?,
    val confidenceLevel: Int?
)

data class PostalAddressWithScriptVariantsRequest(
    val postalProperties: PostalAddressRequest,
    val scriptVariants: List<PostalAddressScriptVariantWithScriptCodeRequest>
)

data class LegalEntityRequest(
    val bpnReference: BpnReferenceRequest,
    val legalName: String?,
    val legalShortName: String?,
    val legalForm: String?,
    val identifiers: List<IdentifierRequest>,
    val states: List<BusinessStateRequest>,
    val confidenceCriteria: ConfidenceCriteriaRequest,
    val isParticipantData: Boolean?,
    val hasChanged: Boolean?,
    val ownershipUltimate: Boolean? = null,
    val ultimateOwnerBpnl: String? = null,
    val legalAddress: PostalAddressRequest,
    val scriptVariants: List<LegalEntityScriptVariantRequest>,
    val goldenRecordRelations: List<LegalEntityGoldenRecordRelationRequest> = emptyList(),
    val updatedAt: Instant? = null
)

data class SiteRequest(
    val bpnReference: BpnReferenceRequest,
    val siteName: String?,
    val states: List<BusinessStateRequest>,
    val confidenceCriteria: ConfidenceCriteriaRequest,
    val hasChanged: Boolean?,
    val siteMainAddress: PostalAddressRequest?,
    val scriptVariants: List<SiteScriptVariantRequest>,
    val goldenRecordRelations: List<SiteGoldenRecordRelationRequest> = emptyList(),
    val updatedAt: Instant? = null
)

data class AdditionalSiteRequest(
    val bpnReference: BpnReferenceRequest,
    val siteName: String?
)

data class UncategorizedPropertiesRequest(
    val nameParts: List<String>,
    val identifiers: List<IdentifierRequest>,
    val states: List<BusinessStateRequest>,
    val address: PostalAddressWithScriptVariantsRequest?
)

data class LegalEntityScriptVariantRequest(
    val scriptCode: String,
    val legalName: String?,
    val legalShortName: String?,
    val legalAddress: PostalAddressScriptVariantRequest
)

data class SiteScriptVariantRequest(
    val scriptCode: String,
    val siteName: String,
    val mainAddress: PostalAddressScriptVariantRequest
)

data class PostalAddressScriptVariantWithScriptCodeRequest(
    val scriptCode: String,
    val postalProperties: PostalAddressScriptVariantRequest
)

data class PostalAddressScriptVariantRequest(
    val addressName: String?,
    val physicalAddress: PhysicalAddressScriptVariantRequest,
    val alternativeAddress: AlternativeAddressScriptVariantRequest?
)

data class PhysicalAddressScriptVariantRequest(
    val city: String?,
    val district: String?,
    val street: StreetScriptVariantRequest,
    val industrialZone: String?,
    val building: String?,
    val floor: String?,
    val door: String?
)

data class AlternativeAddressScriptVariantRequest(
    val city: String?
)

data class StreetScriptVariantRequest(
    val name: String?,
    val direction: String?,
    val namePrefix: String?,
    val additionalNamePrefix: String?,
    val nameSuffix: String?,
    val additionalNameSuffix: String?
)

data class LegalEntityGoldenRecordRelationRequest(
    val relationType: LegalEntityGoldenRecordRelationTypeRequest,
    val sourceBpn: String,
    val targetBpn: String
)

data class SiteGoldenRecordRelationRequest(
    val relationType: SiteGoldenRecordRelationTypeRequest,
    val sourceBpn: String,
    val targetBpn: String
)

data class AddressGoldenRecordRelationRequest(
    val relationType: AddressGoldenRecordRelationTypeRequest,
    val sourceBpn: String,
    val targetBpn: String
)

enum class NamePartTypeRequest {
    LegalName,
    ShortName,
    LegalForm,
    SiteName,
    AddressName
}

enum class BpnReferenceTypeRequest {
    Bpn,
    BpnRequestIdentifier
}

enum class LegalEntityGoldenRecordRelationTypeRequest {
    IsAlternativeHeadquarterFor,
    IsManagedBy,
    IsOwnedBy,
    IsReplacedBy
}

enum class SiteGoldenRecordRelationTypeRequest {
    IsReplacedBy
}

enum class AddressGoldenRecordRelationTypeRequest {
    IsReplacedBy
}
