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

package org.eclipse.tractusx.orchestrator.api.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.*


@Schema(
    description = "Generic business partner with external id"
)
data class BusinessPartnerGenericDto(
    override val nameParts: List<String> = emptyList(),
    override val identifiers: Collection<BusinessPartnerIdentifierDto> = emptyList(),
    override val states: Collection<BusinessPartnerStateDto> = emptyList(),
    override val roles: Collection<BusinessPartnerRole> = emptyList(),
    override val legalEntity: LegalEntityComponent = LegalEntityComponent(),
    override val site: SiteComponent = SiteComponent(),
    override val address: AddressComponent = AddressComponent(),
    @get:Schema(description = "The BPNL of the company sharing and claiming this business partner as its own")
    val ownerBpnL: String? = null
) : IBaseBusinessPartnerDto {
    // Backwards compatibility so changes to this DTO can be done one module at a time
    constructor(
        nameParts: List<String> = emptyList(),
        shortName: String? = null,
        identifiers: Collection<BusinessPartnerIdentifierDto> = emptyList(),
        legalName: String? = null,
        legalForm: String? = null,
        states: Collection<BusinessPartnerStateDto> = emptyList(),
        classifications: Collection<BusinessPartnerClassificationDto> = emptyList(),
        roles: Collection<BusinessPartnerRole> = emptyList(),
        postalAddress: PostalAddressDto = PostalAddressDto(),
        legalEntityBpn: String? = null,
        siteBpn: String? = null,
        addressBpn: String? = null,
        ownerBpnL: String? = null
    ) : this(
        nameParts = nameParts,
        identifiers = identifiers,
        states = states,
        roles = roles,
        ownerBpnL = ownerBpnL,
        legalEntity = LegalEntityComponent(
            bpnL = legalEntityBpn,
            legalName = legalName,
            shortName = shortName,
            legalForm = legalForm,
            classifications = classifications
        ),
        site = SiteComponent(
            bpnS = siteBpn
        ),
        address = AddressComponent(
            bpnA = addressBpn,
            addressType = postalAddress.addressType,
            physicalPostalAddress = postalAddress.physicalPostalAddress,
            alternativePostalAddress = postalAddress.alternativePostalAddress
        )
    )

    override val classifications: Collection<BusinessPartnerClassificationDto>
        @JsonIgnore
        get() = legalEntity.classifications

    override val postalAddress: PostalAddressDto
        @JsonIgnore
        get() = PostalAddressDto(address.addressType, address.physicalPostalAddress, address.alternativePostalAddress)

    fun copy(
        nameParts: List<String> = this.nameParts,
        shortName: String? = this.shortName,
        identifiers: Collection<BusinessPartnerIdentifierDto> = this.identifiers,
        legalName: String? = this.legalName,
        legalForm: String? = this.legalForm,
        states: Collection<BusinessPartnerStateDto> = this.states,
        classifications: Collection<BusinessPartnerClassificationDto> = this.classifications,
        roles: Collection<BusinessPartnerRole> = this.roles,
        postalAddress: PostalAddressDto = this.postalAddress,
        legalEntityBpn: String? = this.legalEntityBpn,
        siteBpn: String? = this.siteBpn,
        addressBpn: String? = this.addressBpn,
        ownerBpnL: String? = this.ownerBpnL
    ) = BusinessPartnerGenericDto(
        nameParts,
        shortName,
        identifiers,
        legalName,
        legalForm,
        states,
        classifications,
        roles,
        postalAddress,
        legalEntityBpn,
        siteBpn,
        addressBpn,
        ownerBpnL
    )
}

data class LegalEntityComponent(
    override val bpnL: String? = null,
    override val legalName: String? = null,
    override val shortName: String? = null,
    override val legalForm: String? = null,
    override val classifications: Collection<BusinessPartnerClassificationDto> = emptyList()
) : IBaseLegalEntityComponent

data class SiteComponent(
    override val bpnS: String? = null
) : IBaseSiteComponent

data class AddressComponent(
    override val bpnA: String? = null,
    override val addressType: AddressType? = null,
    override val physicalPostalAddress: PhysicalPostalAddressDto? = null,
    override val alternativePostalAddress: AlternativePostalAddressDto? = null
) : IBaseAddressComponent


