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
 * SPDX-License-IdentifierV7Dto: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.LegalEntityGoldenRecordRelationV7Dto

@Schema(description = "Legal entity information for this business partner data. " +
        "Every business partner either is a legal entity or belongs to a legal entity." +
        "There, a legal entity property is not allowed to be 'null'. " )
data class LegalEntityV7Dto(
    val bpnReference: BpnReferenceV7Dto,
    @Schema(description = "The legal name of this legal entity according to official registers")
    val legalName: String?,
    @Schema(description = "The abbreviated name of this legal entity, if it exists")
    val legalShortName: String?,
    @Schema(description = "The legal form of this legal entity")
    val legalForm: String?,
    @Schema(description = "Identifiers for this legal entity (in addition to the BPNL)")
    val identifiers: List<IdentifierV7Dto>,
    @Schema(description = "The business state history of this legal entity")
    val states: List<BusinessStateV7Dto>,
    val confidenceCriteria: ConfidenceCriteriaV7Dto,
    @get:Schema(description = "Whether this legal entity is part of the Data Space network", name = "isParticipantData")
    val isParticipantData: Boolean?,
    @Schema(description = "Whether this legal entity information differs from its golden record counterpart in the Pool. +" +
            "The Pool will not update the legal entity if it is set to false. " +
            "However, if this legal entity constitutes a new legal entity golden record, it is still created independent of this flag.")
    val hasChanged: Boolean?,
    @Schema(description = "Whether this legal entity is the ultimate owner in the ownership chain. " +
            "This optional flag is provided by the golden record process and persisted in the Pool.")
    val ownershipUltimate: Boolean? = null,
    @Schema(description = "The BPNL of the ultimate owner in the ownership chain. " +
            "This value is persisted by the Pool and remains empty in this feature.")
    val ultimateOwnerBpnl: String? = null,
    val legalAddress: PostalAddressV7Dto,
    val scriptVariants: List<LegalEntityScriptVariantV7Dto>,
    @Schema(description = "Golden record relations belonging to this legal entity")
    val goldenRecordRelations: List<LegalEntityGoldenRecordRelationV7Dto> = emptyList(),
    val updatedAt: Instant? = null
){
    companion object{
        val empty = LegalEntityV7Dto(
            bpnReference = BpnReferenceV7Dto.empty,
            legalName = null,
            legalShortName = null,
            legalForm = null,
            identifiers = emptyList(),
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteriaV7Dto.empty,
            isParticipantData = null,
            hasChanged = null,
            ownershipUltimate = null,
            ultimateOwnerBpnl = null,
            legalAddress = PostalAddressV7Dto.empty,
            scriptVariants = emptyList(),
            goldenRecordRelations = emptyList(),
            updatedAt = null
        )
    }
}
