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
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.RelationDescription
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.RelationValidityPeriodDescription
import java.time.Instant

@Schema(description = RelationDescription.header)
data class RelationDto(
    @get:Schema(description = RelationDescription.externalId)
    override val externalId: String,
    @get:Schema(description = RelationDescription.relationType)
    override val relationType: RelationType,
    @get:Schema(description = RelationDescription.source)
    override val businessPartnerSourceExternalId: String,
    @get:Schema(description = RelationDescription.target)
    override val businessPartnerTargetExternalId: String,
    @Schema(description = RelationValidityPeriodDescription.header)
    val validityPeriods: Collection<RelationValidityPeriodDto>,
    @get:Schema(description = RelationDescription.reasonCode)
    val reasonCode: String,
    @get:Schema(description = RelationDescription.updatedAt)
    val updatedAt: Instant,
    @get:Schema(description = RelationDescription.createdAt)
    val createdAt: Instant,
): IRelationDto
