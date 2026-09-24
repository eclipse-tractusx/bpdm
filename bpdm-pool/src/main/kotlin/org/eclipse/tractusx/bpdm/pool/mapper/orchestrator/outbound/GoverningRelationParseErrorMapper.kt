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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.pool.model.error.DataManagementUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.GovernedByItself
import org.eclipse.tractusx.bpdm.pool.model.error.GovernedPartnerNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.GoverningPartnerNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.GoverningPartnerRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.GoverningRelationParseError
import org.eclipse.tractusx.bpdm.pool.model.error.ManagedAlreadyManaged
import org.eclipse.tractusx.bpdm.pool.model.error.ManagedIsManager
import org.eclipse.tractusx.bpdm.pool.model.error.ManagerIsManaged
import org.eclipse.tractusx.bpdm.pool.model.error.ManagerNotDataSpaceParticipant
import org.eclipse.tractusx.bpdm.pool.model.error.MultipleUltimateOwnersInHierarchy
import org.eclipse.tractusx.bpdm.pool.model.error.OwnedAlreadyOwned
import org.eclipse.tractusx.bpdm.pool.model.error.OwnershipCycle
import org.eclipse.tractusx.bpdm.pool.model.error.OwnershipPartnerIsAlternativeHeadquarter
import org.eclipse.tractusx.bpdm.pool.model.error.OwnershipUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.PastValidityPeriodAdded
import org.eclipse.tractusx.bpdm.pool.model.error.PastValidityPeriodEndMoved
import org.eclipse.tractusx.bpdm.pool.model.error.PastValidityPeriodRemoved
import org.eclipse.tractusx.bpdm.pool.model.error.RelationReasonCodeNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.RelationValidityPeriodParseError
import org.springframework.stereotype.Component

/**
 * Maps the sealed parse errors of an ownership or data management relation to the error descriptions a golden record
 * relation task reports back.
 *
 * The `when`s are exhaustive so a new error won't compile until it gets a description.
 */
@Component
class GoverningRelationParseErrorMapper(
    private val validityPeriodErrorMapper: RelationValidityPeriodParseErrorMapper
) {

    /**
     * States why the ownership was not written, naming the legal entity it faults.
     */
    fun toUpsertDescription(error: OwnershipUpsertParseError): String =
        when (error) {
            is GoverningRelationParseError -> toSharedDescription(error, ownershipRoles)
            is OwnedAlreadyOwned ->
                "Multiple owning entities assigned to the same owned entity: legal entity '${error.ownedBpn}' is already " +
                        "owned by '${error.existingOwnerBpn}' in an overlapping validity period"
            is OwnershipCycle ->
                "Circular ownership detected in entity hierarchy: legal entity '${error.ownedBpn}' is (transitively) " +
                        "owning '${error.ownerBpn}' and therefore can't be owned by it"
            is OwnershipPartnerIsAlternativeHeadquarter ->
                "Legal entity '${error.bpn}' cannot participate in ownership because it is an alternative headquarter " +
                        "to '${error.mainBpn}' in an overlapping period"
            is MultipleUltimateOwnersInHierarchy ->
                "Multiple ultimate owners in entity hierarchy: this ownership would join legal entities that are each " +
                        "flagged as ultimate owner (flagged: ${error.conflictingBpnls.joinToString(", ")})"
        }

    /**
     * States why the data management relation was not written, naming the legal entity it faults.
     */
    fun toUpsertDescription(error: DataManagementUpsertParseError): String =
        when (error) {
            is GoverningRelationParseError -> toSharedDescription(error, dataManagementRoles)
            is ManagerNotDataSpaceParticipant ->
                "The managing legal entity '${error.bpn}' is not a dataspace participant. Only dataspace participants " +
                        "can manage other entities."
            is ManagedAlreadyManaged ->
                "The managed legal entity '${error.managedBpn}' is already managed by " +
                        "'${error.existingManagerBpns.joinToString(", ")}' in an overlapping validity period. A managed " +
                        "legal entity may only have one managing legal entity at a time."
            is ManagedIsManager ->
                "The legal entity '${error.bpn}' is already a managing legal entity. A managing legal entity cannot " +
                        "also act as a managed legal entity."
            is ManagerIsManaged ->
                "The legal entity '${error.bpn}' is already a managed legal entity. A managed legal entity cannot also " +
                        "act as a managing legal entity."
            is PastValidityPeriodAdded ->
                "Can't add a new relation validity period from ${error.validFrom} as it lies in the past."
            is PastValidityPeriodEndMoved ->
                "Existing relation validity period from ${error.validFrom} can't be limited to past date."
            is PastValidityPeriodRemoved ->
                "Existing relation validity period from ${error.validFrom} can't be deleted as it lies in the past."
        }

    private fun toSharedDescription(error: GoverningRelationParseError, roles: Roles): String =
        when (error) {
            is GovernedPartnerNotFound -> "No legal entity '${error.bpn}' to be ${roles.governedParticiple}"
            is GoverningPartnerNotFound -> "No legal entity '${error.bpn}' to ${roles.governingVerb} it"
            is GovernedByItself -> "A legal entity cannot have a relation to itself (BPNL: ${error.bpn})."
            is RelationReasonCodeNotFound -> "Relation reason code '${error.reasonCode}' not found"
            is GoverningPartnerRecordedInactive ->
                "Legal entity '${error.bpn}' is recorded as inactive during the validity period starting " +
                        "'${error.validFrom}'${error.validTo?.let { " and ending '$it'" } ?: ""}, so it cannot " +
                        "${roles.governingVerb} another legal entity during that period"
            is RelationValidityPeriodParseError -> validityPeriodErrorMapper.toUpsertDescription(error)
        }

    private data class Roles(val governedParticiple: String, val governingVerb: String)

    private val ownershipRoles = Roles(governedParticiple = "owned", governingVerb = "own")

    private val dataManagementRoles = Roles(governedParticiple = "managed", governingVerb = "manage")
}
