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

package org.eclipse.tractusx.bpdm.pool.dto.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SitePartnerResponse
import java.time.Instant

@JsonDeserialize(using = BusinessPartnerResponse.CustomDeserializer::class)
@Schema(name = "Business Partner Response", description = "Business Partner of type legal entity in deprecated response format", deprecated = true)
data class BusinessPartnerResponse(
    val uuid: String,
    @Schema(description = "Business Partner Number, main identifier value for business partners")
    val bpn: String,
    @JsonUnwrapped
    val properties: LegalEntityResponse,
    val addresses: Collection<AddressPartnerResponse>,
    val sites: Collection<SitePartnerResponse>,
    @Schema(description = "The timestamp the business partner data was last indicated to be still current")
    val currentness: Instant
) {
    class CustomDeserializer(vc: Class<BusinessPartnerResponse>?) : StdDeserializer<BusinessPartnerResponse>(vc) {
        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): BusinessPartnerResponse {
            val node = parser.codec.readTree<JsonNode>(parser)

            val addresses = node.get(BusinessPartnerResponse::addresses.name).map { ctxt.readTreeAsValue(it, AddressPartnerResponse::class.java) }
            val sites = node.get(BusinessPartnerResponse::sites.name).map { ctxt.readTreeAsValue(it, SitePartnerResponse::class.java) }

            return BusinessPartnerResponse(
                node.get(BusinessPartnerResponse::uuid.name).textValue(),
                node.get(BusinessPartnerResponse::bpn.name).textValue(),
                ctxt.readTreeAsValue(node, LegalEntityResponse::class.java),
                addresses,
                sites,
                ctxt.readTreeAsValue(node.get(BusinessPartnerResponse::currentness.name), Instant::class.java),
            )
        }
    }
}
