/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "A relation from one business partner (the source) to another business partner (the target). ")
data class RelationDto(
    @Schema(description = "The identifier with which to reference this relation")
    override val externalId: String,
    @Schema(description = "The type of relation between the business partners")
    override val relationType: RelationType,
    @Schema(description = "The business partner from which the relation emerges (the source)")
    override val businessPartnerSourceExternalId: String,
    @Schema(description = "The business partner to which this relation goes (the target)")
    override val businessPartnerTargetExternalId: String,
    @Schema(description = "The time when this relation was last modified")
    val updatedAt: Instant,
    @Schema(description = "The time when this relation was created")
    val createdAt: Instant,
): IRelationDto
