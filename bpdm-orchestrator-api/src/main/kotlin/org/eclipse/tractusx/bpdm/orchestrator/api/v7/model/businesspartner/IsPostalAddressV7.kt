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
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.AddressGoldenRecordRelationV7Dto

interface IsPostalAddressV7{
    val bpnReference: BpnReferenceV7Dto
    @get:Schema( description = "The name of this address")
    val addressName: String?
    @get:Schema(description = "Identifiers for this address (in addition to the BPNA)")
    val identifiers: List<IdentifierV7Dto>
    @get:Schema(description = "The business state history of this address")
    val states: List<BusinessStateV7Dto>
    val confidenceCriteria: ConfidenceCriteriaV7Dto
    val physicalAddress: PhysicalAddressV7Dto
    val alternativeAddress: AlternativeAddressV7Dto?
    @get:Schema(description = "Whether this address information differs from its golden record counterpart in the Pool." +
            "Currently deprecated and ignored by the golden record creation and update process.", deprecated = true)
    val hasChanged: Boolean?
    @get:Schema(description = "Golden record relations belonging to this address")
    val goldenRecordRelations: List<AddressGoldenRecordRelationV7Dto>
    @get:Schema(description = "Timestamp when this address was last updated in the source system")
    val updatedAt: Instant?
}
