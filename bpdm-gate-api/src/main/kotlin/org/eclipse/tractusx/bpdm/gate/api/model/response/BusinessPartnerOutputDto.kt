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

package org.eclipse.tractusx.bpdm.gate.api.model.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.CommonDescription
import org.eclipse.tractusx.bpdm.gate.api.model.*
import java.time.Instant

@Schema(
    description = "Generic business partner output with external id",
    requiredProperties = ["externalId"]
)
data class BusinessPartnerOutputDto(

    override val externalId: String,
    override val nameParts: List<String> = emptyList(),
    override val identifiers: Collection<BusinessPartnerIdentifierDto> = emptyList(),
    override val states: Collection<BusinessPartnerStateDto> = emptyList(),
    override val roles: Collection<BusinessPartnerRole> = emptyList(),
    override val isOwnCompanyData: Boolean = false,
    override val legalEntity: LegalEntityComponentOutputDto,
    override val site: SiteComponentOutputDto = SiteComponentOutputDto(),
    override val address: AddressComponentOutputDto,

    @get:Schema(description = CommonDescription.createdAt)
    val createdAt: Instant,

    @get:Schema(description = CommonDescription.updatedAt)
    val updatedAt: Instant

) : IBaseBusinessPartnerGateDto

@Schema(
    description = "Legal Entity properties of business partner output data",
    requiredProperties = ["bpnL"]
)
data class LegalEntityComponentOutputDto(
    override val bpnL: String,
    override val legalName: String? = null,
    override val shortName: String? = null,
    override val legalForm: String? = null,
    override val classifications: Collection<BusinessPartnerClassificationDto> = emptyList()
) : IBaseLegalEntityComponent

@Schema(
    description = "Site properties of business partner output data"
)
data class SiteComponentOutputDto(
    override val bpnS: String? = null
) : IBaseSiteComponent

@Schema(
    description = "Address properties of business partner output data",
    requiredProperties = ["bpnA"]
)
data class AddressComponentOutputDto(
    override val bpnA: String,
    override val addressType: AddressType? = null,
    override val physicalPostalAddress: PhysicalPostalAddressDto = PhysicalPostalAddressDto(),
    override val alternativePostalAddress: AlternativePostalAddressDto = AlternativePostalAddressDto()
) : IBaseAddressComponent
