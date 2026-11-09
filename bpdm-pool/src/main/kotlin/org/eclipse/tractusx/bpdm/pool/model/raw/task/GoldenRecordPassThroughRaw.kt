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

package org.eclipse.tractusx.bpdm.pool.model.raw.task

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import java.time.Instant

/**
 * The parts of a golden record task the Pool does not interpret, handed back unchanged with the task's result.
 */
data class GoldenRecordPassThroughRaw(
    val nameParts: List<NamePartRaw>,
    val owningCompany: String?,
    val uncategorized: UncategorizedRaw
)

data class NamePartRaw(
    val name: String,
    val type: NamePartKind
)

enum class NamePartKind {
    LegalName,
    ShortName,
    LegalForm,
    SiteName,
    AddressName
}

data class UncategorizedRaw(
    val nameParts: List<String>,
    val identifiers: List<UncategorizedIdentifierRaw>,
    val states: List<BusinessStateRaw>,
    val address: UncategorizedAddressRaw?,
    val scriptVariants: List<UncategorizedScriptVariantRaw>
)

data class UncategorizedScriptVariantRaw(
    val scriptCode: String,
    val nameParts: List<String>
)

data class UncategorizedIdentifierRaw(
    val value: String?,
    val type: String?,
    val issuingBody: String?
)

data class UncategorizedAddressRaw(
    val bpnReferenceValue: String?,
    val bpnReferenceDesiredBpn: String?,
    val bpnReferenceKind: BpnReferenceKind?,
    val addressName: String?,
    val identifiers: List<UncategorizedIdentifierRaw>,
    val states: List<BusinessStateRaw>,
    val confidenceCriteria: UncategorizedConfidenceCriteriaRaw,
    val physicalAddress: UncategorizedPhysicalAddressRaw,
    val alternativeAddress: UncategorizedAlternativeAddressRaw?,
    val hasChanged: Boolean?,
    val relations: List<UncategorizedAddressRelationRaw>,
    val updatedAt: Instant?,
    val scriptVariants: List<UncategorizedAddressScriptVariantRaw>
)

data class UncategorizedConfidenceCriteriaRaw(
    val sharedByOwner: Boolean?,
    val checkedByExternalDataSource: Boolean?,
    val numberOfSharingMembers: Int?,
    val lastConfidenceCheckAt: Instant?,
    val nextConfidenceCheckAt: Instant?,
    val confidenceLevel: Int?
)

data class UncategorizedPhysicalAddressRaw(
    val geographicCoordinates: UncategorizedGeoCoordinateRaw,
    val country: String?,
    val administrativeAreaLevel1: String?,
    val administrativeAreaLevel2: String?,
    val administrativeAreaLevel3: String?,
    val postalCode: String?,
    val city: String?,
    val district: String?,
    val street: UncategorizedStreetRaw,
    val companyPostalCode: String?,
    val industrialZone: String?,
    val building: String?,
    val floor: String?,
    val door: String?,
    val taxJurisdictionCode: String?
)

data class UncategorizedAlternativeAddressRaw(
    val geographicCoordinates: UncategorizedGeoCoordinateRaw,
    val country: String?,
    val administrativeAreaLevel1: String?,
    val postalCode: String?,
    val city: String?,
    val deliveryServiceType: DeliveryServiceType?,
    val deliveryServiceQualifier: String?,
    val deliveryServiceNumber: String?
)

data class UncategorizedGeoCoordinateRaw(
    val longitude: Double?,
    val latitude: Double?,
    val altitude: Double?
)

data class UncategorizedStreetRaw(
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

data class UncategorizedAddressRelationRaw(
    val type: UncategorizedAddressRelationKind,
    val sourceBpn: String,
    val targetBpn: String
)

enum class UncategorizedAddressRelationKind {
    IsReplacedBy
}

data class UncategorizedAddressScriptVariantRaw(
    val scriptCode: String,
    val addressName: String?,
    val physicalAddress: UncategorizedPhysicalAddressScriptVariantRaw,
    val alternativeAddress: UncategorizedAlternativeAddressScriptVariantRaw?
)

data class UncategorizedAlternativeAddressScriptVariantRaw(
    val city: String?
)

data class UncategorizedPhysicalAddressScriptVariantRaw(
    val city: String?,
    val district: String?,
    val street: UncategorizedStreetScriptVariantRaw,
    val industrialZone: String?,
    val building: String?,
    val floor: String?,
    val door: String?
)

data class UncategorizedStreetScriptVariantRaw(
    val name: String?,
    val direction: String?,
    val namePrefix: String?,
    val additionalNamePrefix: String?,
    val nameSuffix: String?,
    val additionalNameSuffix: String?
)
