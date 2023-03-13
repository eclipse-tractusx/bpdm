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
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.AddressDto
import java.time.LocalDateTime

@JsonDeserialize(using = AddressGateInputResponseDeserializer::class)
@Schema(
    name = "AddressGateInputResponse", description = "Address with legal entity or site references. " +
            "Only one of either legal entity or site external id can be set for an address."
)
data class AddressGateInputResponse(
    @field:JsonUnwrapped
    val address: AddressDto,

    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String,

    @Schema(description = "External id of the related legal entity")
    val legalEntityExternalId: String? = null,

    @Schema(description = "External id of the related site")
    val siteExternalId: String? = null,

    @Schema(description = "Business Partner Number")
    val bpn: String? = null,

    @Schema(description = "Time the sharing process was started according to SaaS")
    val processStartedAt: LocalDateTime? = null,
)

private class AddressGateInputResponseDeserializer(vc: Class<AddressGateInputResponse>?) : StdDeserializer<AddressGateInputResponse>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): AddressGateInputResponse {
        val node = parser.codec.readTree<JsonNode>(parser)
        return AddressGateInputResponse(
            address = ctxt.readTreeAsValue(node, AddressDto::class.java),
            externalId = node.get(AddressGateInputResponse::externalId.name).textValue(),
            legalEntityExternalId = node.get(AddressGateInputResponse::legalEntityExternalId.name)?.textValue(),
            siteExternalId = node.get(AddressGateInputResponse::siteExternalId.name)?.textValue(),
            bpn = node.get(AddressGateInputResponse::bpn.name)?.textValue(),
            processStartedAt = node.get(AddressGateInputResponse::processStartedAt.name).let {
                if (it.isNull) null
                else ctxt.readTreeAsValue(it, LocalDateTime::class.java)
            }
        )
    }
}