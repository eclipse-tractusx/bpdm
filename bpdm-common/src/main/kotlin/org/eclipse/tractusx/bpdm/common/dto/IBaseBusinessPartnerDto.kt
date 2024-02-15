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

package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

interface IBaseBusinessPartnerDto {

    @get:ArraySchema(arraySchema = Schema(description = "The list of name parts to accommodate the different number of name fields in different systems."))
    val nameParts: List<String>

    @get:ArraySchema(arraySchema = Schema(description = "The list of identifiers of the business partner. Sorted and duplicates removed by the service."))
    val identifiers: Collection<IBusinessPartnerIdentifierDto>

    @get:ArraySchema(arraySchema = Schema(description = "The list of (temporary) states of the business partner. Sorted and duplicates removed by the service."))
    val states: Collection<IBusinessPartnerStateDto>

    @get:ArraySchema(arraySchema = Schema(description = "One or more of the roles, the business partner assumes with respect to the sharing member: Supplier, Customer. Sorted and duplicates removed by the service."))
    val roles: Collection<BusinessPartnerRole>

    @get:Schema(description = "The legal entity, on which the business partner provides a view.")
    val legalEntity: IBaseLegalEntityRepresentation?

    @get:Schema(description = "The site, on which the business partner provides a view.")
    val site: IBaseSiteRepresentation?

    @get:Schema(description = "The address, on which the business partner provides a view. ")
    val address: IBaseAddressRepresentation?
}

@Schema(description = "A legal entity representation adds context information to the legal entity, on which the business partner provides a view. Additionally, it contains some of the information from the assigned legal entity.")
interface IBaseLegalEntityRepresentation {

    @get:Schema(description = "The BPNL of the legal entity, on which the business partner provides a view.")
    val legalEntityBpn: String?

    @get:Schema(description = "The name of the legal entity, on which the business partner provides a view, according to official registers.")
    val legalName: String?

    @get:Schema(description = "The abbreviated name of the legal entity, on which the business partner provides a view.")
    val shortName: String?

    @get:Schema(description = "The legal form of the legal entity, on which the business partner provides a view.")
    val legalForm: String?

    @get:ArraySchema(arraySchema = Schema(description = "The list of classifications of the business partner, such as a specific industry. Sorted and duplicates removed by the service."))
    val classifications: Collection<IBusinessPartnerClassificationDto>
}

@Schema(description = "A site representation adds context information to the site, on which the business partner provides a view. Additionally, it contains some of the information from the assigned site.")
interface IBaseSiteRepresentation {
    @get:Schema(description = "The BPNS of the site, on which the business partner provides a view.")
    val siteBpn: String?

    @get:Schema(description = "The name of the site, on which the business partner provides a view. This is not according to official registers but according to the name the owner chooses.")
    val name: String?
}

@Schema(description = "An address representation adds context information to the address, on which the business partner provides a view. Additionally, it contains most of the information from the assigned address.")
interface IBaseAddressRepresentation : IBaseBusinessPartnerPostalAddressDto {
    @get:Schema(description = "The BPNA of the address, on which the business partner provides a view.")
    val addressBpn: String?

    @get:Schema(description = "The name of the address, on which the business partner provides a view. This is not according to official registers but according to the name the sharing members agree on, such as the name of a gate or any other additional names that designate the address in common parlance.")
    val name: String?

    @get:Schema(description = "One of the address types: Legal Address, Site Main Address, Legal and Site Main Address, Additional Address. ")
    override val addressType: AddressType?

    @get:Schema(description = "The physical postal address of the address, on which the business partner provides a view, such as an office, warehouse, gate, etc.")
    override val physicalPostalAddress: IBasePhysicalPostalAddressDto?

    @get:Schema(description = "The alternative postal address of the address, on which the business partner provides a view, for example if the goods are to be picked up somewhere else.")
    override val alternativePostalAddress: IBaseAlternativePostalAddressDto?
}