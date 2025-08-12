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

        val sameTargetRelation = existingRelations.find { it.endNode.id == proposedTarget.id }

        if (sameTargetRelation != null) {
            val mergedStates = mergeStates(sameTargetRelation.states, proposedStates)
            return relationUpsertService.upsertRelation(
                RelationUpsertService.UpsertRequest(proposedSource, proposedTarget, RelationType.IsManagedBy, mergedStates)
            )
        }

        // Check for overlaps with different targets
        val conflict = existingRelations.any { existing ->
            existing.states.any { es ->
                proposedStates.any { ps -> overlaps(es.validFrom, es.validTo, ps.validFrom, ps.validTo) }
            }
        }
        if (conflict) {
            throw BpdmValidationException(
                "Invalid 'IsManagedBy' relation: Overlapping manager period exists for source ${proposedSource.bpn}."
            )
        }

        // No overlap — safe to insert new relation
        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(proposedSource, proposedTarget, RelationType.IsManagedBy, proposedStates)
        )

        makeManagedEntityParticipantIfRequired(proposedSource)

        return result
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

    private fun mergeStates(existingStates: List<RelationStateDb>, proposedStates: List<RelationStateDb>): List<RelationStateDb> {
        val merged = existingStates.toMutableList()
        for (ps in proposedStates) {
            val overlapping = merged.find { overlaps(it.validFrom, it.validTo, ps.validFrom, ps.validTo) && it.type == ps.type }
            if (overlapping != null) {
                overlapping.validFrom = minOf(overlapping.validFrom, ps.validFrom)
                overlapping.validTo = maxOf(overlapping.validTo, ps.validTo)
            } else {
                merged.add(ps)
            }
        }
        return merged
    }

}