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

package org.eclipse.tractusx.bpdm.pool.dto.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.AddressResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityResponse
import org.eclipse.tractusx.bpdm.common.service.DataClassUnwrappedJsonDeserializer
import java.time.Instant

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
@Schema(name = "LegalEntityPartnerCreateResponse", description = "Created business partner of type legal entity")
data class LegalEntityPartnerCreateResponse(
    @Schema(description = "Business Partner Number of this legal entity")
    val bpn: String,
    @field:JsonUnwrapped
    val properties: LegalEntityResponse,
    @Schema(description = "The timestamp the business partner data was last indicated to be still current")
    val currentness: Instant,
    @Schema(description = "Address of the official seat of this legal entity")
    val legalAddress: AddressResponse,
    @Schema(description = "User defined index to conveniently match this entry to the corresponding entry from the request")
    val index: String?
)
