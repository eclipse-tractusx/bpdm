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

package org.eclipse.tractusx.orchestrator.api.v6.model

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.PostalAddressDescription
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.StreetDescription
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import java.time.Instant

@Schema(description = "Generic business partner data for golden record processing. " +
        "Typically a sharing member shares incomplete and/or uncategorized business partner data to the golden record process. " +
        "The golden record process categorizes and completes the data in order to create and update the resulting golden records. " +
        "The golden records are found in the legalEntity, site and additionalAddress fields. " +
        "The business partner data needs to contain the full golden record parent relationship. " +
        "This means, if an additional address is specified in the business partner data, also its legal entity and also its site parent (if a site exists) needs to be specified. ")
data class BusinessPartnerV6(
    @Schema(description = "Fully categorized and cleaned name parts based on the uncategorized name parts provided")
    val nameParts: List<NamePartV6>,
    @Schema(description = "The BPNL of the legal entity to which this business partner data belongs to")
    val owningCompany: String?,
    val uncategorized: UncategorizedPropertiesV6,
    val legalEntity: LegalEntityV6,
    val site: SiteV6?,
    val additionalAddress: PostalAddressV6?,
){
    companion object{
        val empty = BusinessPartnerV6(
            nameParts = emptyList(),
            owningCompany = null,
            uncategorized = UncategorizedPropertiesV6.empty,
            legalEntity = LegalEntityV6.empty,
            site = null,
            additionalAddress = null
        )
    }

    @Schema(description = "The recognized golden record type this business partner data contains.\n" +
            "* `Legal Entity`: The business partner data only contains legal entity and legal address information.\n" +
            "* `Site`: The business partner data contains site, site main address and its parent legal entity information.\n" +
            "* `Additional Address`: The business partner data contains an additional address, (optional) parent site and parent legal entity information.\n" +
            "* `Null`: No clear type determined, undecided. The golden record process will not create golden record from this business partner data.")
    val type: GoldenRecordTypeV6? = when{
        additionalAddress != null -> GoldenRecordTypeV6.Address
        site != null -> GoldenRecordTypeV6.Site
        legalEntity != LegalEntityV6.empty -> GoldenRecordTypeV6.LegalEntity
        else -> null
    }
}

@Schema(description = "A categorized name part for this business partner data")
data class NamePartV6(
    @Schema(description = "The name part value")
    val name: String,
    @get:Schema(description = "NamePartType:\n" +
            "* `LegalName` - Value is part of the legal entities' legal name\n" +
            "* `ShortName` - Value is part of the legal entities' short name\n" +
            "* `LegalForm` - Value is part of the legal entities' legal form name\n" +
            "* `SiteName` - Value is part of the site's name\n" +
            "* `AddressName` - Value is part of the address name\n")
    val type: NamePartTypeV6
)

@Schema(description = "The additional identifiers of this business partner (excluding the BPN)")
data class IdentifierV6(
    @get:Schema(description = "Value of the identifier")
    val value: String?,
    @get:Schema(description = "The type of identifier")
    val type: String?,
    @get:Schema(description = "The organisation that issued this identifier")
    val issuingBody: String?
)

@Schema(description = "The business state information for the corresponding business partner")
data class BusinessStateV6(
    @get:Schema(description = "Date since when the status is/was valid.")
    val validFrom: Instant?,
    @get:Schema(description = "Date until the status was valid, if applicable.")
    val validTo: Instant?,
    @get:Schema(description = "The type of this specified status.")
    val type: BusinessStateType?
)

@Schema(description = "A reference to a BPN for the corresponding business partner data. " +
        "Either this reference contains an existing BPN or a BPN request identifier. " +
        "The golden record process makes sure that the each unique BPN request identifier is associated to a unique BPN. " +
        "For a new BPN request identifier the golden record process creates a new BPN (and golden record) and associates it with the BPN request identifier. " +
        "Known BPN request identifiers are resolved to the BPN and the corresponding golden record is updated with the business partner data. " +
        "This makes it possible for duplication check services to reference unique business partners by their own BPN request identifiers instead of needing to rely on BPNs for identification.")
data class BpnReferenceV6(
    @Schema(description =  "The value of the reference, either an existing BPN or a BPN request identifier (which is freely chosen by the duplication check provider)")
    val referenceValue: String?,
    @Schema(description = "The desired BPN when a new golden record has to be created for this business partner data." +
            "Requires a BPN request identifier that is not associated with an existing BPN. " +
            "At this moment the golden record process does not support creating desired BPNs and ignores this field. ")
    val desiredBpn: String?,
    @Schema(description =  "Whether this reference is a BPN or BPN request identifier")
    val referenceType: BpnReferenceTypeV6?
){
    companion object{
        val empty = BpnReferenceV6(null, null, null)
    }
}

@Schema(description = "Address information of this business partner data. " +
        "The address can be either a legal, site main and/or additional address. " +
        "This can also refer to address information which is unknown to which type it belongs to.")
data class PostalAddressV6(
    override val bpnReference: BpnReferenceV6,
    override val addressName: String?,
    override val identifiers: List<IdentifierV6>,
    override val states: List<BusinessStateV6>,
    override val confidenceCriteria: ConfidenceCriteriaV6,
    override val physicalAddress: PhysicalAddressV6,
    override val alternativeAddress: AlternativeAddressV6?,
    override val hasChanged: Boolean?
): IsPostalAddressV6{
    companion object{
        val empty = PostalAddressV6(
            bpnReference = BpnReferenceV6.empty,
            addressName = null,
            identifiers = emptyList(),
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteriaV6.empty,
            physicalAddress = PhysicalAddressV6.empty,
            alternativeAddress = null,
            hasChanged = null
        )
    }
}

@Schema(description = PostalAddressDescription.headerPhysical)
data class PhysicalAddressV6(
    val geographicCoordinates: GeoCoordinateV6,
    @Schema(description = PostalAddressDescription.country)
    val country: String?,
    @Schema(description = PostalAddressDescription.administrativeAreaLevel1)
    val administrativeAreaLevel1: String?,
    @Schema(description = PostalAddressDescription.administrativeAreaLevel2)
    val administrativeAreaLevel2: String?,
    @Schema(description = PostalAddressDescription.administrativeAreaLevel3)
    val administrativeAreaLevel3: String?,
    @Schema(description = PostalAddressDescription.postalCode)
    val postalCode: String?,
    @Schema(description = PostalAddressDescription.city)
    val city: String?,
    @Schema(description = PostalAddressDescription.district)
    val district: String?,
    val street: StreetV6,
    @Schema(description = PostalAddressDescription.companyPostalCode)
    val companyPostalCode: String?,
    @Schema(description = PostalAddressDescription.industrialZone)
    val industrialZone: String?,
    @Schema(description = PostalAddressDescription.building)
    val building: String?,
    @Schema(description = PostalAddressDescription.floor)
    val floor: String?,
    @Schema(description = PostalAddressDescription.door)
    val door: String?,
    @Schema(description = PostalAddressDescription.taxJurisdictionCode)
    val taxJurisdictionCode: String?
){
    companion object {
        val empty: PhysicalAddressV6 = PhysicalAddressV6(
            GeoCoordinateV6.empty, null, null, null,
            null, null, null, null, StreetV6.empty, null, null,
            null, null, null, null
        )
    }
}

@Schema(description = PostalAddressDescription.headerAlternative)
data class AlternativeAddressV6(
    val geographicCoordinates: GeoCoordinateV6,
    @Schema(description = PostalAddressDescription.country)
    val country: String?,
    @Schema(description = PostalAddressDescription.administrativeAreaLevel1)
    val administrativeAreaLevel1: String?,
    @Schema(description = PostalAddressDescription.postalCode)
    val postalCode: String?,
    @Schema(description = PostalAddressDescription.city)
    val city: String?,
    @Schema(description = PostalAddressDescription.deliveryServiceType)
    val deliveryServiceType: DeliveryServiceType?,
    @Schema(description = PostalAddressDescription.deliveryServiceQualifier)
    val deliveryServiceQualifier: String?,
    @Schema(description = PostalAddressDescription.deliveryServiceNumber)
    val deliveryServiceNumber: String?
){
    companion object{
        val empty: AlternativeAddressV6 = AlternativeAddressV6(
            GeoCoordinateV6.empty, null, null,
            null, null, null, null, null)
    }
}

@Schema(description = PostalAddressDescription.headerGeoCoordinates)
data class GeoCoordinateV6(
    val longitude: Double?,
    val latitude: Double?,
    val altitude: Double?
){
    companion object{
        val empty: GeoCoordinateV6 = GeoCoordinateV6(null, null, null)
    }
}

@Schema(description = StreetDescription.header)
data class StreetV6(
    @Schema(description = StreetDescription.name)
    val name: String?,
    @Schema(description = StreetDescription.houseNumber)
    val houseNumber: String?,
    @Schema(description = "The supplement to the house number")
    val houseNumberSupplement: String?,
    @Schema(description = StreetDescription.milestone)
    val milestone: String?,
    @Schema(description = StreetDescription.direction)
    val direction: String?,
    @Schema(description = StreetDescription.namePrefix)
    val namePrefix: String?,
    @Schema(description = StreetDescription.additionalNamePrefix)
    val additionalNamePrefix: String?,
    @Schema(description = StreetDescription.nameSuffix)
    val nameSuffix: String?,
    @Schema(description = StreetDescription.additionalNameSuffix)
    val additionalNameSuffix: String?,
){
    companion object{
        val empty: StreetV6 = StreetV6(null, null, null, null, null, null, null, null, null)
    }
}

@Schema(description = "Contains information to evaluate how good or verified the information in the attached business partner data is. " +
        "This information will be directly written in the matched golden record's confidence criteria.")
data class ConfidenceCriteriaV6(
    @Schema(description = "Whether the business partner data is shared by the actual owner")
    val sharedByOwner: Boolean?,
    @Schema(description = "The corresponding business partner data has been verified by an external data source, like an official register")
    val checkedByExternalDataSource: Boolean?,
    @Schema(description = "How many sharing members have already shared the matched golden record")
    val numberOfSharingMembers: Int?,
    @Schema(description = "Last time the confidence values have been checked (and updated if needed)")
    val lastConfidenceCheckAt: Instant?,
    @Schema(description = "Next time the confidence values should be checked")
    val nextConfidenceCheckAt: Instant?,
    @Schema(description = "The overall confidence level of the matched golden record")
    val confidenceLevel: Int?
){
    companion object{
        val empty = ConfidenceCriteriaV6(
            sharedByOwner = null,
            checkedByExternalDataSource = null,
            numberOfSharingMembers = null,
            lastConfidenceCheckAt = null,
            nextConfidenceCheckAt = null,
            confidenceLevel = null
        )
    }
}

@Schema(description = "Legal entity information for this business partner data. " +
        "Every business partner either is a legal entity or belongs to a legal entity." +
        "There, a legal entity property is not allowed to be 'null'. " )
data class LegalEntityV6(
    val bpnReference: BpnReferenceV6,
    @Schema(description = "The legal name of this legal entity according to official registers")
    val legalName: String?,
    @Schema(description = "The abbreviated name of this legal entity, if it exists")
    val legalShortName: String?,
    @Schema(description = "The legal form of this legal entity")
    val legalForm: String?,
    @Schema(description = "Identifiers for this legal entity (in addition to the BPNL)")
    val identifiers: List<IdentifierV6>,
    @Schema(description = "The business state history of this legal entity")
    val states: List<BusinessStateV6>,
    val confidenceCriteria: ConfidenceCriteriaV6,
    @Schema(description = "Whether this legal entity is part of the Catena-X network")
    val isCatenaXMemberData: Boolean?,
    @Schema(description = "Whether this legal entity information differs from its golden record counterpart in the Pool. +" +
            "The Pool will not update the legal entity if it is set to false. " +
            "However, if this legal entity constitutes a new legal entity golden record, it is still created independent of this flag.")
    val hasChanged: Boolean?,
    val legalAddress: PostalAddressV6
){
    companion object{
        val empty = LegalEntityV6(
            bpnReference = BpnReferenceV6.empty,
            legalName = null,
            legalShortName = null,
            legalForm = null,
            identifiers = emptyList(),
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteriaV6.empty,
            isCatenaXMemberData = null,
            hasChanged = null,
            legalAddress = PostalAddressV6.empty
        )
    }
}

@Schema(description = "Site information for this business partner data. " +
        "A site of 'null' means the business partner data has no site.")
data class SiteV6(
    val bpnReference: BpnReferenceV6,
    @Schema(description = "The name of this site")
    val siteName: String?,
    @Schema(description = "The business state history of this site")
    val states: List<BusinessStateV6>,
    val confidenceCriteria: ConfidenceCriteriaV6,
    @Schema(description = "Whether this site information differs from its golden record counterpart in the Pool. +" +
            "The Pool will not update the site if it is set to false. " +
            "However, if this site constitutes a new site golden record, it is still created independent of this flag.")
    val hasChanged: Boolean?,
    val siteMainAddress: PostalAddressV6?
){
    companion object{
        val empty = SiteV6(
            bpnReference = BpnReferenceV6.empty,
            siteName = null,
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteriaV6.empty,
            hasChanged = null,
            siteMainAddress = null
        )
    }

    @Schema(description =  "This site's main address is the legal address of the legal entity. " +
            "The address information therefore is stored in the legal address.")
    val siteMainIsLegalAddress: Boolean = siteMainAddress == null
}

@Schema(description = "Business partner data that has not yet or can not be categorized")
data class UncategorizedPropertiesV6(
    @Schema(description = "The plain uncategorized name of the business partner how it appears in the sharing member system",)
    val nameParts: List<String>,
    @Schema(description = "Identifiers for which it is unknown whether they belong to the legal entity, site or any address")
    val identifiers: List<IdentifierV6>,
    @Schema(description = "Business states for which it is unknown whether they belong to the legal entity, site or any address")
    val states: List<BusinessStateV6>,
    @Schema(description = "Address information for which it is unknown whether they belong to the legal, site main or additional address")
    val address: PostalAddressV6?
){
    companion object{
        val empty = UncategorizedPropertiesV6(
            nameParts = emptyList(),
            identifiers = emptyList(),
            states = emptyList(),
            address = null
        )
    }
}

enum class NamePartTypeV6{
    LegalName,
    ShortName,
    LegalForm,
    SiteName,
    AddressName
}

enum class GoldenRecordTypeV6{
    LegalEntity,
    Site,
    Address
}

enum class BpnReferenceTypeV6 {
    Bpn,
    BpnRequestIdentifier
}

interface IsPostalAddressV6{
    val bpnReference: BpnReferenceV6
    @get:Schema(description = "The name of this address")
    val addressName: String?
    @get:Schema(description = "Identifiers for this address (in addition to the BPNA)")
    val identifiers: List<IdentifierV6>
    @get:Schema(description = "The business state history of this address")
    val states: List<BusinessStateV6>
    val confidenceCriteria: ConfidenceCriteriaV6
    val physicalAddress: PhysicalAddressV6
    val alternativeAddress: AlternativeAddressV6?
    @get:Schema(description = "Whether this address information differs from its golden record counterpart in the Pool." +
            "Currently deprecated and ignored by the golden record creation and update process.", deprecated = true)
    val hasChanged: Boolean?
}
