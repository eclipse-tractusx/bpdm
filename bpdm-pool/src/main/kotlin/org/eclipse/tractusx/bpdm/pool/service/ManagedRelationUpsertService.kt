/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.api.model.DataSpaceParticipantDto
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.api.model.request.DataSpaceParticipantUpdateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationStateDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ManagedRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository,
    private val dataSpaceParticipantsService: DataSpaceParticipantsService
):IRelationUpsertStrategyService {

    @Transactional
    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb> {
        val (proposedSource, proposedTarget) = upsertRequest

        validateNoChain(proposedSource, proposedTarget)
        validateManagingEntityIsParticipant(proposedTarget)

        val proposedStates = upsertRequest.states.map { dto ->
            RelationStateDb(
                validFrom = dto.validFrom,
                validTo = dto.validTo,
                type = dto.type
            )
        }

        val existingRelations = relationRepository.findByTypeAndStartNode(RelationType.IsManagedBy, proposedSource)

        // === 1. SAME TARGET EXISTS ===
        val sameTargetRelation = existingRelations.find { it.endNode.id == proposedTarget.id }
        if (sameTargetRelation != null) {
            // Scenario 2: Always replace states with new proposed states
            return relationUpsertService.upsertRelation(
                RelationUpsertService.UpsertRequest(proposedSource, proposedTarget, RelationType.IsManagedBy, proposedStates)
            )
        }

        // === 2. DIFFERENT TARGET EXISTS ===
        val conflictWithDifferentTarget = existingRelations.any { existing ->
            existing.endNode.id != proposedTarget.id && hasOverlap(existing.states, proposedStates)
        }
        if (conflictWithDifferentTarget) {
            // Scenario 3: Overlap with different target -> exception
            throw BpdmValidationException(
                "Invalid 'IsManagedBy' relation: Overlapping manager period exists for source ${proposedSource.bpn}."
            )
        }

        // === 3. NO CONFLICT — create new relation ===
        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(proposedSource, proposedTarget, RelationType.IsManagedBy, proposedStates)
        )

        makeManagedEntityParticipantIfRequired(proposedSource)

        return result
    }

    private fun hasOverlap(existingStates: Collection<RelationStateDb>, proposedStates: Collection<RelationStateDb>): Boolean {
        return existingStates.any { es ->
            proposedStates.any { ps -> overlaps(es.validFrom, es.validTo, ps.validFrom, ps.validTo) }
        }
    }

    private fun validateNoChain(source: LegalEntityDb, target: LegalEntityDb) {
        val sourceRelations = relationRepository.findInSourceOrTarget(RelationType.IsManagedBy, source)
        val targetRelations = relationRepository.findInSourceOrTarget(RelationType.IsManagedBy, target)

        val sourceIsTarget = sourceRelations.any { it.endNode.id == source.id }
        val targetIsSource = targetRelations.any { it.startNode.id == target.id }

        when {
            sourceIsTarget -> throw BpdmValidationException(
                "Invalid 'IsManagedBy' relation: The legal entity with BPNL '${source.bpn}' is already a Managing Legal Entity. " +
                        "A Managing Legal Entity cannot also act as a Managed Legal Entity."
            )
            targetIsSource -> throw BpdmValidationException(
                "Invalid 'IsManagedBy' relation: The legal entity with BPNL '${target.bpn}' is already a Managed Legal Entity. " +
                        "A Managed Legal Entity cannot also act as a Managing Legal Entity."
            )
        }
    }

    private fun validateManagingEntityIsParticipant(target: LegalEntityDb) {
        if (!target.isCatenaXMemberData) {
            throw BpdmValidationException(
                "Invalid 'IsManagedBy' relation: The Managing Legal Entity with BPNL '${target.bpn}' is not a dataspace participant. " +
                        "Only dataspace participants can manage other entities."
            )
        }
    }

    private fun makeManagedEntityParticipantIfRequired(source: LegalEntityDb) {
        if (!source.isCatenaXMemberData) {
            dataSpaceParticipantsService.updateMemberships(
                DataSpaceParticipantUpdateRequest(
                    listOf(DataSpaceParticipantDto(source.bpn, true))
                )
            )
        }
    }

    private fun overlaps(from1: Instant, to1: Instant, from2: Instant, to2: Instant): Boolean {
        return !to1.isBefore(from2) && !from1.isAfter(to2)
    }
}