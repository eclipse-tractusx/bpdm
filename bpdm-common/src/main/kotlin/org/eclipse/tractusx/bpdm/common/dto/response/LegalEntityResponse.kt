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

package org.eclipse.tractusx.bpdm.common.dto.response

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

// TODO probably rename to LegalEntityDetailedDto
// TODO can we add bpn?
@Schema(name = "LegalEntityResponse", description = "Legal entity record")
data class LegalEntityResponse(
    @ArraySchema(arraySchema = Schema(description = "All identifiers of the business partner, including BPN information"))
    val identifiers: Collection<IdentifierResponse> = emptyList(),

    @Schema(description = "Legal name the partner goes by")
    val legalName: NameResponse,

    @Schema(description = "Legal form of the business partner")
    val legalForm: LegalFormResponse? = null,

    @ArraySchema(arraySchema = Schema(description = "Business status"))
    val status: Collection<BusinessStatusResponse> = emptyList(),

    @ArraySchema(arraySchema = Schema(description = "Classifications"))
    val classifications: Collection<ClassificationResponse> = emptyList(),

    @ArraySchema(arraySchema = Schema(description = "Relations to other business partners"))
    val relations: Collection<RelationResponse> = emptyList(),
)
