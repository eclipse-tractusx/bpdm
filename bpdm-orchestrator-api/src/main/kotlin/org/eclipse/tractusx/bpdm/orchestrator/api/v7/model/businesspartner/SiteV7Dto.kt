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
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.SiteGoldenRecordRelationV7Dto

@Schema(description = "Site information for this business partner data. " +
        "A site of 'null' means the business partner data has no site.")
data class SiteV7Dto(
    val bpnReference: BpnReferenceV7Dto,
    @Schema(description = "The name of this site")
    val siteName: String?,
    @Schema(description = "The business state history of this site")
    val states: List<BusinessStateV7Dto>,
    val confidenceCriteria: ConfidenceCriteriaV7Dto,
    @Schema(description = "Whether this site information differs from its golden record counterpart in the Pool. +" +
            "The Pool will not update the site if it is set to false. " +
            "However, if this site constitutes a new site golden record, it is still created independent of this flag.")
    val hasChanged: Boolean?,
    val siteMainAddress: PostalAddressV7Dto?,
    val scriptVariants: List<SiteScriptVariantV7Dto>,
    @Schema(description = "Golden record relations belonging to this site")
    val goldenRecordRelations: List<SiteGoldenRecordRelationV7Dto> = emptyList(),
    val updatedAt: Instant? = null
){
    companion object{
        val empty = SiteV7Dto(
            bpnReference = BpnReferenceV7Dto.empty,
            siteName = null,
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteriaV7Dto.empty,
            hasChanged = null,
            siteMainAddress = PostalAddressV7Dto.empty,
            scriptVariants = emptyList(),
            goldenRecordRelations = emptyList(),
            updatedAt = null
        )
    }

    @Schema(description =  "This site's main address is the legal address of the legal entity. " +
            "The address information therefore is stored in the legal address.")
    val siteMainIsLegalAddress = siteMainAddress == null
}
