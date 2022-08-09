/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.response


import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType

@Schema(name = "Legal Entity Response", description = "Legal entity record")
data class LegalEntityResponse(
    @Schema(description = "Business Partner Number, main identifier value for business partners")
    val bpn: String?,
    @ArraySchema(arraySchema = Schema(description = "All identifiers of the business partner, including BPN information"))
    val identifiers: Collection<IdentifierResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Names the partner goes by"))
    val names: Collection<NameResponse> = emptyList(),
    @Schema(description = "Legal form of the business partner")
    val legalForm: LegalFormResponse? = null,
    @Schema(description = "Current business status")
    val status: BusinessStatusResponse? = null,
    @ArraySchema(arraySchema = Schema(description = "Profile classifications"))
    val profileClassifications: Collection<ClassificationResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "The partner types"))
    val types: Collection<TypeKeyNameUrlDto<BusinessPartnerType>> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Bank accounts of this partner"))
    val bankAccounts: Collection<BankAccountResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Roles the partner takes in the Catena network"))
    val roles: Collection<TypeKeyNameDto<String>> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Relations to other business partners"))
    val relations: Collection<RelationResponse> = emptyList(),
    @Schema(description = "Address of the official seat of this legal entity")
    val legalAddress: AddressResponse
)