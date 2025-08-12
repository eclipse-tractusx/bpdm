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

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationStateDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime

@Service
class OwnedByRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository
): IRelationUpsertStrategyService {

    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb> {
        val proposedSource = upsertRequest.source
        val proposedTarget = upsertRequest.target

        val computedStates = upsertRequest.states.map { dto ->
            RelationStateDb(
                validFrom = dto.validFrom,
                validTo = dto.validTo,
                type = dto.type
            )
        }

        validateSingleParent(proposedSource, proposedTarget)
        validateNoCycles(proposedSource, proposedTarget)

        val existingRelations = relationRepository.findByTypeAndStartNode(RelationType.IsOwnedBy, proposedSource)
        val sameTargetRelation = existingRelations.find { it.endNode == proposedTarget }

        return if (sameTargetRelation != null) {
            val existingState = sameTargetRelation.states.single() // assuming one per relation
            val proposedState = computedStates.single()

            if (periodsOverlap(existingState.validFrom, existingState.validTo, proposedState.validFrom, proposedState.validTo)) {
                // Merge
                val mergedState = RelationStateDb(
                    validFrom = minOf(existingState.validFrom, proposedState.validFrom),
                    validTo = maxOf(existingState.validTo, proposedState.validTo),
                    type = proposedState.type // assume latest type wins
                )
                sameTargetRelation.states.clear()
                sameTargetRelation.states.add(mergedState)
                relationRepository.save(sameTargetRelation)
                UpsertResult(sameTargetRelation, UpsertType.Updated)
            } else {
                // Append as new valid period
                sameTargetRelation.states.add(proposedState)
                relationRepository.save(sameTargetRelation)
                UpsertResult(sameTargetRelation, UpsertType.Updated)
            }
        } else {
            // No same target → create new relation
            relationUpsertService.upsertRelation(
                RelationUpsertService.UpsertRequest(proposedSource, proposedTarget, RelationType.IsOwnedBy, computedStates)
            )
        }
    }

    private fun periodsOverlap(s1: Instant, e1: Instant, s2: Instant, e2: Instant) = !e1.isBefore(s2) && !e2.isBefore(s1)

    private fun validateSingleParent(child: LegalEntityDb, parent: LegalEntityDb){
        val allChildRelations = relationRepository.findByTypeAndStartNode(RelationType.IsOwnedBy, child)
        allChildRelations.forEach { relation ->
            if(relation.endNode != parent)
                throw BpdmValidationException("Multiple owning entities assigned to the same owned entity: legal entity '${child.bpn}' can't be owned by '${parent.bpn}' as its already owned by '${relation.endNode.bpn}'")
        }
    }

    private fun validateNoCycles(child: LegalEntityDb, parent: LegalEntityDb){
        val allOwningAncestors = getAllAncestors(parent)
        if(allOwningAncestors.contains(child))
            throw BpdmValidationException("Circular ownership detected in entity hierarchy: legal entity '${child.bpn}' is (transitively) owning '${parent.bpn}' and therefore can't be owned by '${parent.bpn}'.")
    }

    /**
     * Fetch the whole owning parent tree (ancestors, parents of parents) of the given legal entity
     */
    private fun getAllAncestors(legalEntity: LegalEntityDb): Set<LegalEntityDb>{
        val allParents = mutableSetOf<LegalEntityDb>()
        val parentProcessingQueue =  ArrayDeque<LegalEntityDb>()

        parentProcessingQueue.addFirst(legalEntity)

        lateinit var currentParent: LegalEntityDb

        do{
            currentParent = parentProcessingQueue.removeFirst()
            allParents.add(currentParent)
            // Fetch Relations in which the currently processed parent node is a child
            val parentParentRelations = relationRepository.findByTypeAndStartNode(RelationType.IsOwnedBy, currentParent)
            val parentsOfCurrent = parentParentRelations.map { it.endNode }
            parentsOfCurrent.forEach { parentProcessingQueue.addFirst(it) }
        }while (parentProcessingQueue.isNotEmpty())

        return allParents
    }
}