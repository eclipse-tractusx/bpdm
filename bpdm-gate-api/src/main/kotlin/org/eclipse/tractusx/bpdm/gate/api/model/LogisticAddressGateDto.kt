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
import org.eclipse.tractusx.bpdm.common.dto.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.AddressStateDto
import org.eclipse.tractusx.bpdm.common.dto.AlternativePostalAddressDto


@Schema(name = "LogisticAddressGateDto", description = "Address record for a business partner")
data class LogisticAddressGateDto(
    @get:Schema(
        description = "Name of the logistic address of the business partner. This is not according to official\n" +
                "registers but according to the name the uploading sharing member chooses."
    )
    val nameParts: Collection<String> = emptyList(),

    @ArraySchema(arraySchema = Schema(description = "Indicates if the LogisticAddress is \"Active\" or \"Inactive\"."))
    val states: Collection<AddressStateDto> = emptyList(),

    @ArraySchema(arraySchema = Schema(description = "List of identifiers"))
    val identifiers: Collection<AddressIdentifierDto> = emptyList(),

    @get:Schema(description = "Physical postal address")
    val physicalPostalAddress: PhysicalPostalAddressGateDto,

    @get:Schema(description = "Alternative postal address")
    val alternativePostalAddress: AlternativePostalAddressDto? = null
)