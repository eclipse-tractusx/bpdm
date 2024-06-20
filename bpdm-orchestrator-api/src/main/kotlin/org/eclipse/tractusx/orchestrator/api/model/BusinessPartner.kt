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

package org.eclipse.tractusx.orchestrator.api.model

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import java.time.Instant


data class BusinessPartner(
    val nameParts: List<NamePart>,
    val owningCompany: String?,
    val uncategorized: UncategorizedProperties,
    val legalEntity: LegalEntity,
    val site: Site?,
    val additionalAddress: PostalAddress?,
){
    companion object{
        val empty = BusinessPartner(
            nameParts = emptyList(),
            owningCompany = null,
            uncategorized = UncategorizedProperties.empty,
            legalEntity = LegalEntity.empty,
            site = null,
            additionalAddress = null
        )
    }

    val type: GoldenRecordType? = when{
        additionalAddress != null -> GoldenRecordType.Address
        site != null -> GoldenRecordType.Site
        legalEntity != LegalEntity.empty -> GoldenRecordType.LegalEntity
        else -> null
    }
}

data class NamePart(
    val name: String,
    val type: NamePartType
)

data class Identifier(
    val value: String?,
    val type: String?,
    val issuingBody: String?
)

data class BusinessState(
    val validFrom: Instant?,
    val validTo: Instant?,
    val type: BusinessStateType?
)

data class BpnReference(
    val referenceValue: String?,
    val desiredBpn: String?,
    val referenceType: BpnReferenceType?
){
    companion object{
        val empty = BpnReference(null, null, null)
    }
}

data class PostalAddress(
    val bpnReference: BpnReference,
    val addressName: String?,
    val identifiers: List<Identifier>,
    val states: List<BusinessState>,
    val confidenceCriteria: ConfidenceCriteria,
    val physicalAddress: PhysicalAddress,
    val alternativeAddress: AlternativeAddress?,
    val hasChanged: Boolean?
){
    companion object{
        val empty = PostalAddress(
            bpnReference = BpnReference.empty,
            addressName = null,
            identifiers = emptyList(),
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteria.empty,
            physicalAddress = PhysicalAddress.empty,
            alternativeAddress = null,
            hasChanged = null
        )
    }
}

data class PhysicalAddress(
    val geographicCoordinates: GeoCoordinate,
    val country: String?,
    val administrativeAreaLevel1: String?,
    val administrativeAreaLevel2: String?,
    val administrativeAreaLevel3: String?,
    val postalCode: String?,
    val city: String?,
    val district: String?,
    val street: Street,
    val companyPostalCode: String?,
    val industrialZone: String?,
    val building: String?,
    val floor: String?,
    val door: String?,
    val taxJurisdictionCode: String?
){
    companion object {
        val empty: PhysicalAddress = PhysicalAddress(
            GeoCoordinate.empty, null, null, null,
            null, null, null, null, Street.empty, null, null,
            null, null, null, null
        )
    }
}

data class AlternativeAddress(
    val geographicCoordinates: GeoCoordinate,
    val country: String?,
    val administrativeAreaLevel1: String?,
    val postalCode: String?,
    val city: String?,
    val deliveryServiceType: DeliveryServiceType?,
    val deliveryServiceQualifier: String?,
    val deliveryServiceNumber: String?
){
    companion object{
        val empty: AlternativeAddress = AlternativeAddress(
            GeoCoordinate.empty, null, null,
            null, null, null, null, null)
    }
}

data class GeoCoordinate(
    val longitude: Float?,
    val latitude: Float?,
    val altitude: Float?
){
    companion object{
        val empty: GeoCoordinate = GeoCoordinate(null, null, null)
    }
}

data class Street(
    val name: String?,
    val houseNumber: String?,
    val houseNumberSupplement: String?,
    val milestone: String?,
    val direction: String?,
    val namePrefix: String?,
    val additionalNamePrefix: String?,
    val nameSuffix: String?,
    val additionalNameSuffix: String?,
){
    companion object{
        val empty: Street = Street(null, null, null, null, null, null, null, null, null)
    }
}

data class ConfidenceCriteria(
    val sharedByOwner: Boolean?,
    val checkedByExternalDataSource: Boolean?,
    val numberOfSharingMembers: Int?,
    val lastConfidenceCheckAt: Instant?,
    val nextConfidenceCheckAt: Instant?,
    val confidenceLevel: Int?
){
    companion object{
        val empty = ConfidenceCriteria(
            sharedByOwner = null,
            checkedByExternalDataSource = null,
            numberOfSharingMembers = null,
            lastConfidenceCheckAt = null,
            nextConfidenceCheckAt = null,
            confidenceLevel = null
        )
    }
}

data class AdditionalAddress(
    val addressBpn: BpnReference,
    val hasChanged: Boolean?,
    val postalProperties: PostalAddress
)

data class LegalEntity(
    val bpnReference: BpnReference,
    val legalName: String?,
    val legalShortName: String?,
    val legalForm: String?,
    val identifiers: List<Identifier>,
    val states: List<BusinessState>,
    val confidenceCriteria: ConfidenceCriteria,
    val isCatenaXMemberData: Boolean?,
    val hasChanged: Boolean?,
    val legalAddress: PostalAddress
){
    companion object{
        val empty = LegalEntity(
            bpnReference = BpnReference.empty,
            legalName = null,
            legalShortName = null,
            legalForm = null,
            identifiers = emptyList(),
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteria.empty,
            isCatenaXMemberData = null,
            hasChanged = null,
            legalAddress = PostalAddress.empty
        )
    }
}

data class Site(
    val bpnReference: BpnReference,
    val siteName: String?,
    val states: List<BusinessState>,
    val confidenceCriteria: ConfidenceCriteria,
    val hasChanged: Boolean?,
    val siteMainAddress: PostalAddress?
){
    companion object{
        val empty = Site(
            bpnReference = BpnReference.empty,
            siteName = null,
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteria.empty,
            hasChanged = null,
            siteMainAddress = PostalAddress.empty
        )
    }

    val siteMainIsLegalAddress = siteMainAddress == null
}


data class UncategorizedProperties(
    val nameParts: List<String>,
    val identifiers: List<Identifier>,
    val states: List<BusinessState>,
    val address: PostalAddress?
){
    companion object{
        val empty = UncategorizedProperties(
            nameParts = emptyList(),
            identifiers = emptyList(),
            states = emptyList(),
            address = null
        )
    }
}

enum class NamePartType{
    LegalName,
    ShortName,
    LegalForm,
    SiteName,
    AddressName
}

enum class GoldenRecordType{
    LegalEntity,
    Site,
    Address
}

enum class BpnReferenceType {
    Bpn,
    BpnRequestIdentifier
}