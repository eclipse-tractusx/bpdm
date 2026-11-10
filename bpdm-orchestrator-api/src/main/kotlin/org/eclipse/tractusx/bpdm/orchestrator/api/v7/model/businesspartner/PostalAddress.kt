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

package org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.AddressGoldenRecordRelation

@Schema(description = "Address information of this business partner data. " +
        "The address can be either a legal, site main and/or additional address. " +
        "This can also refer to address information which is unknown to which type it belongs to.")
data class PostalAddress(
    override val bpnReference: BpnReference,
    override val addressName: String?,
    override val identifiers: List<Identifier>,
    override val states: List<BusinessState>,
    override val confidenceCriteria: ConfidenceCriteria,
    override val physicalAddress: PhysicalAddress,
    override val alternativeAddress: AlternativeAddress?,
    override val hasChanged: Boolean?,
    override val goldenRecordRelations: List<AddressGoldenRecordRelation> = emptyList(),
    override val updatedAt: Instant? = null
): IsPostalAddress{
    companion object{
        val empty = PostalAddress(
            bpnReference = BpnReference.empty,
            addressName = null,
            identifiers = emptyList(),
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteria.empty,
            physicalAddress = PhysicalAddress.empty,
            alternativeAddress = null,
            hasChanged = null,
            goldenRecordRelations = emptyList(),
            updatedAt = null
        )
    }
}
