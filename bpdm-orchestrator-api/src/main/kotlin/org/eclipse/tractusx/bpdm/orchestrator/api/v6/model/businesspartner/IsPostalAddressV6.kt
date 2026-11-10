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

interface IsPostalAddressV6{
    val bpnReference: BpnReferenceV6Dto
    @get:Schema(description = "The name of this address")
    val addressName: String?
    @get:Schema(description = "Identifiers for this address (in addition to the BPNA)")
    val identifiers: List<IdentifierV6Dto>
    @get:Schema(description = "The business state history of this address")
    val states: List<BusinessStateV6Dto>
    val confidenceCriteria: ConfidenceCriteriaV6Dto
    val physicalAddress: PhysicalAddressV6Dto
    val alternativeAddress: AlternativeAddressV6Dto?
    @get:Schema(description = "Whether this address information differs from its golden record counterpart in the Pool." +
            "Currently deprecated and ignored by the golden record creation and update process.", deprecated = true)
    val hasChanged: Boolean?
}
