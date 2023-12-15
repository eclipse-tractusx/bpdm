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

package org.eclipse.tractusx.bpdm.gate.api.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.IBaseBusinessPartnerDto
import org.eclipse.tractusx.bpdm.common.dto.IBaseBusinessPartnerPostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.IBusinessPartnerClassificationDto
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.CommonDescription

interface IBaseBusinessPartnerGateDto : IBaseBusinessPartnerDto {

    @get:Schema(description = CommonDescription.externalId)
    val externalId: String

    @get:Schema(description = "True if the sharing member declares itself as the owner of the business partner.")
    val isOwnCompanyData: Boolean

    val legalEntity: IBaseLegalEntityComponent

    val site: IBaseSiteComponent

    val address: IBaseAddressComponent

    // Overrides to satisfy the base class but will be not shown on API level.
    // That way we inherit from the base but keep the Gate model changes separate from now
    // ToDo: Once all other BPDM module models are adapted, update the Base Interface and delete the overrides

    override val legalEntityBpn: String?
        @JsonIgnore
        get() = legalEntity.bpnL

    override val legalName: String?
        @JsonIgnore
        get() = legalEntity.legalName

    override val shortName: String?
        @JsonIgnore
        get() = legalEntity.shortName

    override val legalForm: String?
        @JsonIgnore
        get() = legalEntity.legalForm

    override val classifications: Collection<IBusinessPartnerClassificationDto>
        @JsonIgnore
        get() = legalEntity.classifications

    override val siteBpn: String?
        @JsonIgnore
        get() = site.bpnS

    override val addressBpn: String?
        @JsonIgnore
        get() = address.bpnA

    override val postalAddress: IBaseBusinessPartnerPostalAddressDto
        @JsonIgnore
        get() = address
}

interface IBaseLegalEntityComponent {

    @get:Schema(description = "BPNL of the golden record legal entity this business partner refers to")
    val bpnL: String?

    @get:Schema(description = "The name according to official registers.")
    val legalName: String?

    @get:Schema(description = "Abbreviated name or shorthand.")
    val shortName: String?

    @get:Schema(description = "Technical key of the legal form.")
    val legalForm: String?

    @get:ArraySchema(arraySchema = Schema(description = "The list of classifications of the business partner, such as a specific industry. Sorted and duplicates removed by the service."))
    val classifications: Collection<IBusinessPartnerClassificationDto>
}

interface IBaseSiteComponent {
    @get:Schema(description = "BPNS of the golden record site this business partner refers to")
    val bpnS: String?
}

interface IBaseAddressComponent : IBaseBusinessPartnerPostalAddressDto {
    @get:Schema(description = "BPNA of the golden record address this business partner refers to")
    val bpnA: String?
}