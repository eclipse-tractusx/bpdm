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
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.LogisticAddressDescription

@Schema(description = LogisticAddressDescription.header)
data class LogisticAddressGateDto(

    @get:ArraySchema(arraySchema = Schema(description = LogisticAddressDescription.nameParts))
    val nameParts: List<String> = emptyList(),

    @get:ArraySchema(arraySchema = Schema(description = LogisticAddressDescription.states))
    val states: List<AddressStateDto> = emptyList(),

    @get:ArraySchema(arraySchema = Schema(description = LogisticAddressDescription.identifiers))
    val identifiers: List<AddressIdentifierDto> = emptyList(),

    // TODO OpenAPI description for complex field does not work!!
    @get:Schema(description = LogisticAddressDescription.physicalPostalAddress)
    val physicalPostalAddress: PhysicalPostalAddressGateDto,

    // TODO OpenAPI description for complex field does not work!!
    @get:Schema(description = LogisticAddressDescription.alternativePostalAddress)
    val alternativePostalAddress: AlternativePostalAddressGateDto? = null,

    @get:ArraySchema(arraySchema = Schema(description = LogisticAddressDescription.roles))
    val roles: List<BusinessPartnerRole> = emptyList()
)
