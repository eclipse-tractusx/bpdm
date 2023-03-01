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
import org.eclipse.tractusx.bpdm.common.dto.SiteDto
import java.time.LocalDateTime

@JsonDeserialize(using = SiteGateInputResponseDeserializer::class)
@Schema(
    name = "SiteGateInputResponse", description = "Site with legal entity reference"
)
data class SiteGateInputResponse(
    @field:JsonUnwrapped
    val site: SiteDto,

    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String,

    @Schema(description = "External id of the related legal entity")
    val legalEntityExternalId: String,

    @Schema(description = "Business Partner Number")
    val bpn: String?,

    @Schema(description = "Time the sharing process was started according to SaaS")
    val processStartedAt: LocalDateTime? = null,
)

private class SiteGateInputResponseDeserializer(vc: Class<SiteGateInputResponse>?) : StdDeserializer<SiteGateInputResponse>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): SiteGateInputResponse {
        val node = parser.codec.readTree<JsonNode>(parser)
        return SiteGateInputResponse(
            site = ctxt.readTreeAsValue(node, SiteDto::class.java),
            externalId = node.get(SiteGateInputResponse::externalId.name).textValue(),
            legalEntityExternalId = node.get(SiteGateInputResponse::legalEntityExternalId.name)?.textValue()!!,
            bpn = node.get(SiteGateInputResponse::bpn.name)?.textValue(),
            processStartedAt = node.get(SiteGateInputResponse::processStartedAt.name).let {
                if (it.isNull) null
                else ctxt.readTreeAsValue(it, LocalDateTime::class.java)
            }
        )
    }
}