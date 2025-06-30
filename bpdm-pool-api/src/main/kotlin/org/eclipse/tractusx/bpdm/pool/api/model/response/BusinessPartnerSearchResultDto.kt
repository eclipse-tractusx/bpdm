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

package org.eclipse.tractusx.bpdm.pool.api.model.response

import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.api.model.StreetDto
import java.time.LocalDateTime

@Schema(name = "BusinessPartnerSearchResultDto", description = "Response of the business partner search result.")
data class BusinessPartnerSearchResultDto(
    val identifiers: Collection<BusinessPartnerIdentifierDto> = emptyList(),
    val states: Collection<BusinessPartnerStateDto> = emptyList(),
    val legalEntity: BusinessPartnerLegalEntity,
    val site: BusinessPartnerSite?,
    val address: BusinessPartnerPostalAddress,
    val isParticipantData: Boolean
)

data class BusinessPartnerLegalEntity(
    val legalEntityBpn: String,
    val legalName: String? = null,
    val shortName: String? = null,
    val legalForm: String? = null,
    val confidenceCriteria: BusinessPartnerConfidenceCriteriaDto,
    val states: Collection<BusinessPartnerStateDto> = emptyList()
)

data class BusinessPartnerSite(
    val siteBpn: String,
    val name: String? = null,
    val confidenceCriteria: BusinessPartnerConfidenceCriteriaDto,
    val states: Collection<BusinessPartnerStateDto> = emptyList()
)

data class BusinessPartnerIdentifierDto(

    @get:Schema(description = "The type of the identifier.")
    val type: String?,
    @get:Schema(description = "The value of the identifier like “DE123465789.")
    val value: String?,
    @get:Schema(description = "The name of the official register, where the identifier is registered. For example, a Handelsregisternummer in Germany is only valid with its corresponding Registergericht and Registerart.")
    val issuingBody: String?

)

data class BusinessPartnerPostalAddress(
    val addressBpn: String,
    val name: String? = null,
    val addressType: AddressType?,
    val physicalPostalAddress: PhysicalPostalAddressDto = PhysicalPostalAddressDto(),
    val alternativePostalAddress: AlternativePostalAddressDto? = null,
    val confidenceCriteria: BusinessPartnerConfidenceCriteriaDto,
    val states: Collection<BusinessPartnerStateDto> = emptyList(),
    val identifiers: Collection<AddressIdentifierDto> = emptyList(),
)

data class BusinessPartnerStateDto(
    val validFrom: LocalDateTime?,
    val validTo: LocalDateTime?,
    val type: BusinessStateType?
)

data class PhysicalPostalAddressDto(
    val geographicCoordinates: GeoCoordinateDto? = null,
    val country: CountryCode? = null,
    val administrativeAreaLevel1: String? = null,
    val administrativeAreaLevel2: String? = null,
    val administrativeAreaLevel3: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val district: String? = null,
    val street: StreetDto? = null,
    val companyPostalCode: String? = null,
    val industrialZone: String? = null,
    val building: String? = null,
    val floor: String? = null,
    val door: String? = null,
    val taxJurisdictionCode: String? = null
)

data class AlternativePostalAddressDto(
    val geographicCoordinates: GeoCoordinateDto? = null,
    val country: CountryCode? = null,
    val administrativeAreaLevel1: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val deliveryServiceType: DeliveryServiceType? = null,
    val deliveryServiceQualifier: String? = null,
    val deliveryServiceNumber: String? = null
)

data class BusinessPartnerConfidenceCriteriaDto(
    val sharedByOwner: Boolean,
    val checkedByExternalDataSource: Boolean,
    val numberOfSharingMembers: Int,
    val lastConfidenceCheckAt: LocalDateTime,
    val nextConfidenceCheckAt: LocalDateTime,
    val confidenceLevel: Int
)

data class AddressIdentifierDto(
    val value: String,
    val type: String
)