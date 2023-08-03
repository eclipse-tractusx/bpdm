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

package org.eclipse.tractusx.bpdm.pool.api.model.request

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.IdentifierBusinessPartnerType

@Schema(name = "IdentifiersSearchRequest", description = "Contains identifiers to search legal entities by")
data class IdentifiersSearchRequest(

    @Schema(description = "Legal entities (L) or addresses (A)")
    val businessPartnerType: IdentifierBusinessPartnerType,

    @Schema(description = "Technical key of the type to which the identifiers belongs to")
    val idType: String,

    @Schema(description = "Values of the identifiers")
    val idValues: List<String>
)