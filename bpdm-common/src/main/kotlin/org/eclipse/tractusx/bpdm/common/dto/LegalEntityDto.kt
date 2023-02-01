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

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType

@Schema(name = "LegalEntity")
data class LegalEntityDto(
    @ArraySchema(arraySchema = Schema(description = "Additional identifiers (except BPN)", required = false))
    val identifiers: Collection<IdentifierDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Names the partner goes by"), minItems = 1)
    val names: Collection<NameDto>,
    @Schema(description = "Technical key of the legal form")
    val legalForm: String? = null,
    @Schema(description = "Current business status")
    val status: BusinessStatusDto?,
    @ArraySchema(arraySchema = Schema(description = "Profile classifications", required = false))
    val profileClassifications: Collection<ClassificationDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "The type of partner", required = false, defaultValue = "[]"))
    val types: Collection<BusinessPartnerType> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Bank accounts of this partner", required = false))
    val bankAccounts: Collection<BankAccountDto> = emptyList(),
    @Schema(description = "Address of the official seat of this legal entity")
    val legalAddress: AddressDto
)
