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

package org.eclipse.tractusx.orchestrator.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "The business partner relations data to be processed ")
data class BusinessPartnerRelations(
    @Schema(description = "The type of relation between the business partners")
    val relationType: RelationType,
    @Schema(description = "The business partner from which the relation emerges (the source)")
    val businessPartnerSourceBpnl: String,
    @Schema(description = "The business partner to which this relation goes (the target)")
    val businessPartnerTargetBpnl: String,
    @Schema(description = "Timestamp from which the relation is valid (inclusive)")
    val validFrom: Instant,
    @Schema(description = "Timestamp until which the relation is valid (inclusive)")
    val validTo: Instant
) {
    companion object {
        val empty = BusinessPartnerRelations(
            relationType = RelationType.IsAlternativeHeadquarterFor, // or a default type
            businessPartnerSourceBpnl = "",
            businessPartnerTargetBpnl = "",
            validFrom = Instant.EPOCH,
            validTo = Instant.EPOCH,
        )
    }
}

@Schema(description = "The business partner relations data processed ")
data class BusinessPartnerRelationVerboseDto(
    @Schema(description = "The type of relation between the business partners")
    val relationType: RelationType,
    @Schema(description = "The business partner from which the relation emerges (the source)")
    val businessPartnerSourceBpnl: String,
    @Schema(description = "The business partner to which this relation goes (the target)")
    val businessPartnerTargetBpnl: String,
    @Schema(description = "Timestamp from which the relation is valid (inclusive)")
    val validFrom: Instant,
    @Schema(description = "Timestamp until which the relation is valid (inclusive)")
    val validTo: Instant,
    @Schema(description = "Status of the relation")
    val isActive: Boolean
) {
    companion object {
        val empty = BusinessPartnerRelationVerboseDto(
            relationType = RelationType.IsAlternativeHeadquarterFor, // or a default type
            businessPartnerSourceBpnl = "",
            businessPartnerTargetBpnl = "",
            validFrom = Instant.EPOCH,
            validTo = Instant.EPOCH,
            isActive = false // or a default type
        )
    }
}

enum class RelationType {
    IsAlternativeHeadquarterFor,
    IsManagedBy
}