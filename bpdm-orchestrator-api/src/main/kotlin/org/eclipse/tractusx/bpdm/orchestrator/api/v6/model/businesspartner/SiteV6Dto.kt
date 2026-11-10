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

@Schema(description = "Site information for this business partner data. " +
        "A site of 'null' means the business partner data has no site.")
data class SiteV6Dto(
    val bpnReference: BpnReferenceV6Dto,
    @Schema(description = "The name of this site")
    val siteName: String?,
    @Schema(description = "The business state history of this site")
    val states: List<BusinessStateV6Dto>,
    val confidenceCriteria: ConfidenceCriteriaV6Dto,
    @Schema(description = "Whether this site information differs from its golden record counterpart in the Pool. +" +
            "The Pool will not update the site if it is set to false. " +
            "However, if this site constitutes a new site golden record, it is still created independent of this flag.")
    val hasChanged: Boolean?,
    val siteMainAddress: PostalAddressV6Dto?
){
    companion object{
        val empty = SiteV6Dto(
            bpnReference = BpnReferenceV6Dto.empty,
            siteName = null,
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteriaV6Dto.empty,
            hasChanged = null,
            siteMainAddress = null
        )
    }

    @Schema(description =  "This site's main address is the legal address of the legal entity. " +
            "The address information therefore is stored in the legal address.")
    val siteMainIsLegalAddress: Boolean = siteMainAddress == null
}
