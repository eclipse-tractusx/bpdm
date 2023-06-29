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
import java.time.Instant


@Schema(name = "LogisticAddressResponse", description = "Logistic address ")
data class LogisticAddressResponse(

    @get:Schema(description = "Business Partner Number of this address")
    val bpna: String,

    @get:Schema(
        description = "Name of the logistic address of the business partner. This is not according to official\n" +
                "registers but according to the name the uploading sharing member chooses."
    )
    val name: String? = null,

    @ArraySchema(arraySchema = Schema(description = "Address status"))
    val states: Collection<AddressStateVerboseDto> = emptyList(),

    @ArraySchema(arraySchema = Schema(description = "All identifiers of the Address"))
    val identifiers: Collection<AddressIdentifierVerboseDto> = emptyList(),

    @get:Schema(description = "Physical postal address")
    val physicalPostalAddress: PhysicalPostalAddressVerboseDto,

    @get:Schema(description = "Alternative postal address")
    val alternativePostalAddress: AlternativePostalAddressVerboseDto? = null,

    @get:Schema(description = "BPN of the related legal entity, if available")
    val bpnLegalEntity: String?,

    @get:Schema(name = "isLegalAddress", description = "Flag if this is the legal address of its related legal entity")
    val isLegalAddress: Boolean = false,

    @get:Schema(description = "BPN of the related site, if available")
    val bpnSite: String?,

    @get:Schema(name = "isMainAddress", description = "Flag if this is the main address of its related site")
    val isMainAddress: Boolean = false,

    @get:Schema(description = "The timestamp the business partner data was created")
    val createdAt: Instant,

    @get:Schema(description = "The timestamp the business partner data was last updated")
    val updatedAt: Instant
)