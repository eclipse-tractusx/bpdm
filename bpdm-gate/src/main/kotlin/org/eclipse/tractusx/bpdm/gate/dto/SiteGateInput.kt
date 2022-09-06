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
import org.eclipse.tractusx.bpdm.common.dto.SiteDto

@JsonDeserialize(using = SiteGateInputDeserializer::class)
@Schema(
    name = "Site Gate Input", description = " Site with legal entity reference ."
)
data class SiteGateInput(
    @Schema(description = "Business Partner Number")
    val bpn: String?,
    @JsonUnwrapped
    val site: SiteDto,
    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String,
    @Schema(description = "External id of the related legal entity")
    val legalEntityExternalId: String,
)

class SiteGateInputDeserializer(vc: Class<SiteGateInput>?) : StdDeserializer<SiteGateInput>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): SiteGateInput {
        val node = parser.codec.readTree<JsonNode>(parser)
        return SiteGateInput(
            node.get(SiteGateInput::bpn.name)?.textValue(),
            ctxt.readTreeAsValue(node, SiteDto::class.java),
            node.get(SiteGateInput::externalId.name).textValue(),
            node.get(SiteGateInput::legalEntityExternalId.name)?.textValue()!!
        )
    }
}