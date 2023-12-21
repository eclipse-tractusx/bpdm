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

package org.eclipse.tractusx.bpdm.common.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

interface IBaseBusinessPartnerDto {

    @get:ArraySchema(arraySchema = Schema(description = "The list of name parts to accommodate the different number of name fields in different systems."))
    val nameParts: List<String>

    @get:ArraySchema(arraySchema = Schema(description = "The list of identifiers of the business partner. Sorted and duplicates removed by the service."))
    val identifiers: Collection<IBusinessPartnerIdentifierDto>

    @get:ArraySchema(arraySchema = Schema(description = "The list of (temporary) states of the business partner. Sorted and duplicates removed by the service."))
    val states: Collection<IBusinessPartnerStateDto>

    @get:ArraySchema(arraySchema = Schema(description = "Roles this business partner takes in relation to the sharing member. Sorted and duplicates removed by the service."))
    val roles: Collection<BusinessPartnerRole>

    val legalEntity: IBaseLegalEntityComponent

    val site: IBaseSiteComponent

    val address: IBaseAddressComponent

    // Overrides to satisfy the base class but will be not shown on API level.
    // That way other modules using this business partner are still backwards compatible and can be adapted one after another
    // ToDo: Once all other BPDM module models and mappings are adapted, update the Base Interface and delete the overrides

    val legalEntityBpn: String?
        @JsonIgnore
        get() = legalEntity.bpnL

    val legalName: String?
        @JsonIgnore
        get() = legalEntity.legalName

    val shortName: String?
        @JsonIgnore
        get() = legalEntity.shortName

    val legalForm: String?
        @JsonIgnore
        get() = legalEntity.legalForm

    val classifications: Collection<IBusinessPartnerClassificationDto>
        @JsonIgnore
        get() = legalEntity.classifications

    val siteBpn: String?
        @JsonIgnore
        get() = site.bpnS

    val addressBpn: String?
        @JsonIgnore
        get() = address.bpnA

    val postalAddress: IBaseBusinessPartnerPostalAddressDto
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