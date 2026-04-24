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

package org.eclipse.tractusx.orchestrator.api.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A golden record relation between legal entities, pointing from a source BPNL to a target BPNL")
data class LegalEntityGoldenRecordRelation(
    @Schema(description = "The type of this relation")
    val relationType: LegalEntityGoldenRecordRelationType,
    @Schema(description = "The BPNL of the source legal entity")
    val sourceBpn: String,
    @Schema(description = "The BPNL of the target legal entity")
    val targetBpn: String
)

enum class LegalEntityGoldenRecordRelationType {
    IsAlternativeHeadquarterFor,
    IsManagedBy,
    IsOwnedBy
}

@Schema(description = "A golden record relation between addresses, pointing from a source BPNA to a target BPNA")
data class AddressGoldenRecordRelation(
    @Schema(description = "The type of this relation")
    val relationType: AddressGoldenRecordRelationType,
    @Schema(description = "The BPNA of the source address")
    val sourceBpn: String,
    @Schema(description = "The BPNA of the target address")
    val targetBpn: String
)

enum class AddressGoldenRecordRelationType {
    IsReplacedBy
}
