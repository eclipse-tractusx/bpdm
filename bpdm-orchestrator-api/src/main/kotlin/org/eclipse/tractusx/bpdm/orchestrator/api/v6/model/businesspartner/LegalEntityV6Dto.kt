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

package org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Legal entity information for this business partner data. " +
        "Every business partner either is a legal entity or belongs to a legal entity." +
        "There, a legal entity property is not allowed to be 'null'. " )
data class LegalEntityV6Dto(
    val bpnReference: BpnReferenceV6Dto,
    @Schema(description = "The legal name of this legal entity according to official registers")
    val legalName: String?,
    @Schema(description = "The abbreviated name of this legal entity, if it exists")
    val legalShortName: String?,
    @Schema(description = "The legal form of this legal entity")
    val legalForm: String?,
    @Schema(description = "Identifiers for this legal entity (in addition to the BPNL)")
    val identifiers: List<IdentifierV6Dto>,
    @Schema(description = "The business state history of this legal entity")
    val states: List<BusinessStateV6Dto>,
    val confidenceCriteria: ConfidenceCriteriaV6Dto,
    @Schema(description = "Whether this legal entity is part of the Catena-X network")
    val isCatenaXMemberData: Boolean?,
    @Schema(description = "Whether this legal entity information differs from its golden record counterpart in the Pool. +" +
            "The Pool will not update the legal entity if it is set to false. " +
            "However, if this legal entity constitutes a new legal entity golden record, it is still created independent of this flag.")
    val hasChanged: Boolean?,
    val legalAddress: PostalAddressV6Dto
){
    companion object{
        val empty = LegalEntityV6Dto(
            bpnReference = BpnReferenceV6Dto.empty,
            legalName = null,
            legalShortName = null,
            legalForm = null,
            identifiers = emptyList(),
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteriaV6Dto.empty,
            isCatenaXMemberData = null,
            hasChanged = null,
            legalAddress = PostalAddressV6Dto.empty
        )
    }
}
