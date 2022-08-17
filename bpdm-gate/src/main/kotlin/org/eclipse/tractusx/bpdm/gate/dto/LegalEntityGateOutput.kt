/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.dto.response.AddressResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityResponse

@JsonDeserialize(using = LegalEntityGateOutputDeserializer::class)
@Schema(name = "Legal Entity Gate Output", description = "Legal entity with references")
data class LegalEntityGateOutput(
    @JsonUnwrapped
    val legalEntity: LegalEntityResponse,
    @Schema(description = "Address of the official seat of this legal entity")
    val legalAddress: AddressResponse,
    @Schema(description = "Business Partner Number, main identifier value for business partners")
    val bpn: String?,
    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String
)

class LegalEntityGateOutputDeserializer(vc: Class<LegalEntityGateOutput>?) : StdDeserializer<LegalEntityGateOutput>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): LegalEntityGateOutput {
        val node = parser.codec.readTree<JsonNode>(parser)
        return LegalEntityGateOutput(
            ctxt.readTreeAsValue(node, LegalEntityResponse::class.java),
            ctxt.readTreeAsValue(node.get(LegalEntityGateOutput::legalAddress.name), AddressResponse::class.java),
            node.get(LegalEntityGateOutput::bpn.name).textValue(),
            node.get(LegalEntityGateOutput::externalId.name).textValue()
        )
    }
}