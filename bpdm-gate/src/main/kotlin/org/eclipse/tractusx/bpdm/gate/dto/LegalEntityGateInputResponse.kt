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

package org.eclipse.tractusx.bpdm.gate.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityDto
import org.eclipse.tractusx.bpdm.common.service.DataClassUnwrappedJsonDeserializer
import java.time.LocalDateTime

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
@Schema(name = "LegalEntityGateInputResponse", description = "Legal entity with external id")
data class LegalEntityGateInputResponse(
    @field:JsonUnwrapped
    val legalEntity: LegalEntityDto,

    @Schema(description = "ID the record has in the external system where the record originates from", required = true)
    val externalId: String,

    @Schema(description = "Business Partner Number")
    val bpn: String?,

    @Schema(description = "Time the sharing process was started according to SaaS")
    val processStartedAt: LocalDateTime? = null,
)

