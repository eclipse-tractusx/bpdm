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

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant


@Schema(name = "LogisticAddressResponse", description = "Logistic address ")
data class LogisticAddressResponse(
    @Schema(description = "Business Partner Number of this address")
    @JsonProperty("bpn")
    val bpn: String,

    @Schema(
        description = "Name of the logistic address of the business partner. This is not according to official\n" +
                "registers but according to the name the uploading sharing member chooses."
    )
    @JsonProperty("name")
    val name: String? = null,

    @ArraySchema(arraySchema = Schema(description = "Address status"))
    val states: Collection<AddressStateResponse> = emptyList(),

    @ArraySchema(arraySchema = Schema(description = "All identifiers of the Address"))
    val identifiers: Collection<AddressIdentifierResponse> = emptyList(),

    @Schema(description = "Physical postal address")
    val physicalPostalAddress: PhysicalPostalAddressResponse,

    @Schema(description = "Alternative postal address")
    val alternativePostalAddress: AlternativePostalAddressResponse? = null,

    @Schema(description = "BPN of the related legal entity, if available")
    @JsonProperty("bpnLegalEntity")
    val bpnLegalEntity: String?,

    @Schema(description = "Flag if this is the legal address of its related legal entity")
    @get:JsonProperty("isLegalAddress")
    val isLegalAddress: Boolean = false,

    @Schema(description = "BPN of the related site, if available")
    @JsonProperty("bpnSite")
    val bpnSite: String?,

    @Schema(description = "Flag if this is the main address of its related site")
    @get:JsonProperty("isMainAddress")
    val isMainAddress: Boolean = false,

    @Schema(description = "The timestamp the business partner data was created")
    @JsonProperty("createdAt")
    val createdAt: Instant,

    @Schema(description = "The timestamp the business partner data was last updated")
    @JsonProperty("updatedAt")
    val updatedAt: Instant
)