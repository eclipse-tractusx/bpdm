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
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SitePartnerResponse
import org.eclipse.tractusx.bpdm.common.service.DataClassUnwrappedJsonDeserializer
import java.time.Instant

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
@Schema(name = "BusinessPartnerResponse", description = "Business Partner of type legal entity in deprecated response format", deprecated = true)
data class BusinessPartnerResponse(
    val uuid: String,
    @Schema(description = "Business Partner Number, main identifier value for business partners")
    val bpn: String,
    @field:JsonUnwrapped
    val properties: LegalEntityResponse,
    val addresses: Collection<AddressPartnerResponse>,
    val sites: Collection<SitePartnerResponse>,
    @Schema(description = "The timestamp the business partner data was last indicated to be still current")
    val currentness: Instant
)
