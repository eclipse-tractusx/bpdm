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

import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.AddressGoldenRecordRelation

@Schema(description = "The additional address information of this business partner data. " +
        "An additional address is an address that is distinct from the legal and site main address. " +
        "The additional address is either an additional address of the legal entity (site is null) or of the site (site is not null). " +
        "An additional address of 'Null' means the business partner data has no additional address")
data class PostalAddressWithScriptVariants(
    @field:JsonUnwrapped
    val postalProperties: PostalAddress,
    val scriptVariants: List<PostalAddressScriptVariantWithScriptCode>,
): IsPostalAddress{
    companion object{
        val empty = PostalAddressWithScriptVariants(
            postalProperties = PostalAddress.empty,
            scriptVariants = emptyList()
        )
    }

    fun copyAsPostalAddress(transform: (PostalAddress) -> PostalAddress): PostalAddressWithScriptVariants{
        return copy(postalProperties = transform(postalProperties))
    }

    fun toPostalAddress() =
        PostalAddress(
            bpnReference = bpnReference,
            addressName = addressName,
            identifiers = identifiers,
            states = states,
            confidenceCriteria = confidenceCriteria,
            physicalAddress = physicalAddress,
            alternativeAddress = alternativeAddress,
            hasChanged = hasChanged,
            goldenRecordRelations = postalProperties.goldenRecordRelations,
            updatedAt = postalProperties.updatedAt
        )

    override val bpnReference: BpnReference
        get() = postalProperties.bpnReference

    override val addressName: String?
        get() = postalProperties.addressName

    override val identifiers: List<Identifier>
        get() = postalProperties.identifiers
    override val states: List<BusinessState>
        get() = postalProperties.states

    override val confidenceCriteria: ConfidenceCriteria
        get() = postalProperties.confidenceCriteria

    override val physicalAddress: PhysicalAddress
        get() = postalProperties.physicalAddress

    override val alternativeAddress: AlternativeAddress?
        get() = postalProperties.alternativeAddress

    override val hasChanged: Boolean?
        get() = postalProperties.hasChanged

    override val goldenRecordRelations: List<AddressGoldenRecordRelation>
        get() = postalProperties.goldenRecordRelations

    override val updatedAt: Instant?
        get() = postalProperties.updatedAt
}
