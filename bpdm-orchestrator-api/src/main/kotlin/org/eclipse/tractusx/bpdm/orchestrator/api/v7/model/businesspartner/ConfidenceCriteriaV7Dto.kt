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

@Schema(description = "Contains information to evaluate how good or verified the information in the attached business partner data is. " +
        "This information will be directly written in the matched golden record's confidence criteria.")
data class ConfidenceCriteriaV7Dto(
    @Schema(description = "Whether the business partner data is shared by the actual owner")
    val sharedByOwner: Boolean?,
    @Schema(description = "The corresponding business partner data has been verified by an external data source, like an official register")
    val checkedByExternalDataSource: Boolean?,
    @Schema(description = "How many sharing members have already shared the matched golden record")
    val numberOfSharingMembers: Int?,
    @Schema(description = "Last time the confidence values have been checked (and updated if needed)")
    val lastConfidenceCheckAt: Instant?,
    @Schema(description = "Next time the confidence values should be checked")
    val nextConfidenceCheckAt: Instant?,
    @Schema(description = "The overall confidence level of the matched golden record")
    val confidenceLevel: Int?
){
    companion object{
        val empty = ConfidenceCriteriaV7Dto(
            sharedByOwner = null,
            checkedByExternalDataSource = null,
            numberOfSharingMembers = null,
            lastConfidenceCheckAt = null,
            nextConfidenceCheckAt = null,
            confidenceLevel = null
        )
    }
}
