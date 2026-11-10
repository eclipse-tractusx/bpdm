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

package org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.RelationDescription
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.RelationValidityPeriodDescription

@Schema(description = "The business partner relations data to be processed ")
data class BusinessPartnerRelationsV7Dto(
    @Schema(description = "The type of relation between the business partners")
    val relationType: RelationTypeV7,
    @Schema(description = "The business partner from which the relation emerges (the source)")
    val businessPartnerSourceBpn: String,
    @Schema(description = "The business partner to which this relation goes (the target)")
    val businessPartnerTargetBpn: String,
    @Schema(description = RelationValidityPeriodDescription.header)
    val validityPeriods : Collection<RelationValidityPeriodV7Dto>,
    @Schema(description = RelationDescription.reasonCode)
    val reasonCode: String?,
) {
    companion object {
        val empty = BusinessPartnerRelationsV7Dto(
            relationType = RelationTypeV7.IsAlternativeHeadquarterFor, // or a default type
            businessPartnerSourceBpn = "",
            businessPartnerTargetBpn = "",
            validityPeriods = emptyList(),
            reasonCode = null
        )
    }
}
