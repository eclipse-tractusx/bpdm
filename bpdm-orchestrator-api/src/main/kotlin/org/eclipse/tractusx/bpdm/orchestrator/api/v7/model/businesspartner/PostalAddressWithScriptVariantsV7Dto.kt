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

import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.AddressGoldenRecordRelationV7Dto

@Schema(description = "The additional address information of this business partner data. " +
        "An additional address is an address that is distinct from the legal and site main address. " +
        "The additional address is either an additional address of the legal entity (site is null) or of the site (site is not null). " +
        "An additional address of 'Null' means the business partner data has no additional address")
data class PostalAddressWithScriptVariantsV7Dto(
    @field:JsonUnwrapped
    val postalProperties: PostalAddressV7Dto,
    val scriptVariants: List<PostalAddressScriptVariantWithScriptCodeV7Dto>,
): IsPostalAddressV7{
    companion object{
        val empty = PostalAddressWithScriptVariantsV7Dto(
            postalProperties = PostalAddressV7Dto.empty,
            scriptVariants = emptyList()
        )
    }

    fun copyAsPostalAddress(transform: (PostalAddressV7Dto) -> PostalAddressV7Dto): PostalAddressWithScriptVariantsV7Dto{
        return copy(postalProperties = transform(postalProperties))
    }

    fun toPostalAddress() =
        PostalAddressV7Dto(
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

    override val bpnReference: BpnReferenceV7Dto
        get() = postalProperties.bpnReference

    override val addressName: String?
        get() = postalProperties.addressName

    override val identifiers: List<IdentifierV7Dto>
        get() = postalProperties.identifiers
    override val states: List<BusinessStateV7Dto>
        get() = postalProperties.states

    override val confidenceCriteria: ConfidenceCriteriaV7Dto
        get() = postalProperties.confidenceCriteria

    override val physicalAddress: PhysicalAddressV7Dto
        get() = postalProperties.physicalAddress

    override val alternativeAddress: AlternativeAddressV7Dto?
        get() = postalProperties.alternativeAddress

    override val hasChanged: Boolean?
        get() = postalProperties.hasChanged

    override val goldenRecordRelations: List<AddressGoldenRecordRelationV7Dto>
        get() = postalProperties.goldenRecordRelations

    override val updatedAt: Instant?
        get() = postalProperties.updatedAt
}
