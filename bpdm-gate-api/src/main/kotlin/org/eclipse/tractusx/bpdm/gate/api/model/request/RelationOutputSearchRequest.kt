/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.api.model.request

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import java.time.Instant

@Schema(description = "Request payload containing search parameters for business partner output relations")
data class RelationOutputSearchRequest(
    @Schema(description = "Only show relations with the given external identifiers")
    val externalIds: List<String>? = null,
    @Schema(description = "Only show relations of the given type")
    val relationType: RelationType? = null,
    @Schema(description = "Only show relations which have the given business partners as sources")
    val sourceBpnLs: List<String>? = null,
    @Schema(description = "Only show relations which have the given business partners as targets")
    val targetBpnLs: List<String>? = null,
    @Schema(description = "Only show relations which have been modified after the given time stamp")
    val updatedAtFrom: Instant? = null,
)
