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

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalFormDto
import org.eclipse.tractusx.bpdm.pool.api.model.StreetDto

@Schema(name = "BusinessPartnerSearchResultDto", description = "")
data class BusinessPartnerSearchResultDto(
    @Schema(description = "BPN L/S/A")
    val id: String,

    @Schema(description = "BPN L/S/A")
    val name: String?,

    @Schema(description = "Legal Form")
    val legalForm: LegalFormDto?,

    @Schema(description = "Street")
    val street: StreetDto?,

    @Schema(description = "City")
    val city: String,

    @Schema(description = "PostalCode")
    val postalCode: String,

    @Schema(description = "Country")
    val country: String,

    @Schema(description = "")
    val identifiers: List<LegalEntityIdentifierDto>
)
