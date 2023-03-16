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

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.AddressDto
import org.eclipse.tractusx.bpdm.common.dto.IdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.NameDto

data class BusinessPartnerCandidateDto(
    @ArraySchema(arraySchema = Schema(description = "Identifiers of this partner candidate", required = false))
    val identifiers: Collection<IdentifierDto> = emptyList(),

    @ArraySchema(arraySchema = Schema(description = "Names the partner goes by"), minItems = 1)
    val names: Collection<NameDto>,

    @Schema(description = "Technical key of the legal form")
    val legalForm: String? = null,

    @Schema(description = "Address of this partner")
    val address: AddressDto
)
