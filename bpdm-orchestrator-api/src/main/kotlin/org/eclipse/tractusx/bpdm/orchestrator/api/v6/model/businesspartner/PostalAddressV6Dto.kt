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

@Schema(description = "Address information of this business partner data. " +
        "The address can be either a legal, site main and/or additional address. " +
        "This can also refer to address information which is unknown to which type it belongs to.")
data class PostalAddressV6Dto(
    override val bpnReference: BpnReferenceV6Dto,
    override val addressName: String?,
    override val identifiers: List<IdentifierV6Dto>,
    override val states: List<BusinessStateV6Dto>,
    override val confidenceCriteria: ConfidenceCriteriaV6Dto,
    override val physicalAddress: PhysicalAddressV6Dto,
    override val alternativeAddress: AlternativeAddressV6Dto?,
    override val hasChanged: Boolean?
): IsPostalAddressV6{
    companion object{
        val empty = PostalAddressV6Dto(
            bpnReference = BpnReferenceV6Dto.empty,
            addressName = null,
            identifiers = emptyList(),
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteriaV6Dto.empty,
            physicalAddress = PhysicalAddressV6Dto.empty,
            alternativeAddress = null,
            hasChanged = null
        )
    }
}
