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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityRelationEventTriggerDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.TriggerEventType
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRelationEventTriggerRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OwnedByRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository,
    private val ultimateOwnerResolutionService: UltimateOwnerResolutionService,
    private val legalEntityRelationEventTriggerRepository: LegalEntityRelationEventTriggerRepository
): IRelationUpsertStrategyService {


    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb> {
        val proposedSource = upsertRequest.source
        val proposedTarget = upsertRequest.target

        validateSingleParent(upsertRequest)
        validateNoCycles(upsertRequest)


        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(
                source = proposedSource,
                target = proposedTarget,
                legalEntityRelationType = LegalEntityRelationType.IsOwnedBy,
                validityPeriods = upsertRequest.validityPeriods,
                existingRelation = upsertRequest.existingRelation,
                reasonCode = upsertRequest.reasonCode,
            )
        )

        ultimateOwnerResolutionService.updateUltimateOwnerForEntityAndDescendants(proposedSource)

        handleValidityBoundaryTriggers(result.relation)

        return result
    }

    /**
     * Reconcile the unprocessed OwnershipValidityBoundary triggers for the relation against the set derived
     * from its current validity periods:
     * - validFrom > today  →  trigger on validFrom (relation becomes active)
     * - validTo != null && validTo+1 > today  →  trigger on validTo+1 (relation expires)
     *
     * Only the difference is applied (delete stale dates, insert missing ones); triggers whose date is
     * unchanged are left in place. A blind delete+reinsert would collide on the
     * (relation_id, event_type, trigger_date) unique constraint, because Hibernate flushes inserts before
     * deletes within a transaction — so re-upserting a relation with unchanged trigger dates would fail.
     */
    private fun handleValidityBoundaryTriggers(relation: RelationDb) {
        val today = LocalDate.now()

        val validFromDates = relation.validityPeriods
            .map { it.validFrom }
            .filter { it > today }

        val expiryDates = relation.validityPeriods
            .mapNotNull { it.validTo }
            .map { it.plusDays(1) }
            .filter { it > today }

        val desiredTriggerDates = (validFromDates + expiryDates).toSet()

        val existingUnprocessedTriggers = legalEntityRelationEventTriggerRepository
            .findByRelationAndEventType(relation, TriggerEventType.OwnershipValidityBoundary)
            .filterNot { it.isProcessed }
        val existingTriggerDates = existingUnprocessedTriggers.map { it.triggerDate }.toSet()

        val triggersToDelete = existingUnprocessedTriggers.filterNot { it.triggerDate in desiredTriggerDates }
        val triggerDatesToCreate = desiredTriggerDates - existingTriggerDates

        legalEntityRelationEventTriggerRepository.deleteAll(triggersToDelete)
        legalEntityRelationEventTriggerRepository.saveAll(
            triggerDatesToCreate.map { LegalEntityRelationEventTriggerDb(it, false, TriggerEventType.OwnershipValidityBoundary, relation) }
        )
    }

    private fun validateSingleParent(upsertRequest: IRelationUpsertStrategyService.UpsertRequest){
        val child = upsertRequest.source
        val parent = upsertRequest.target

        val allChildRelations = relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsOwnedBy, child)
        val allOverlappingChildRelations = relationUpsertService.filterOverlappingRelations(upsertRequest, allChildRelations)

        allOverlappingChildRelations.forEach { relation ->
            if(relation.endNode != parent)
                throw BpdmValidationException("Multiple owning entities assigned to the same owned entity: legal entity '${child.bpn}' can't be owned by '${parent.bpn}' as its already owned by '${relation.endNode.bpn}'")
        }
    }

    private fun validateNoCycles(upsertRequest: IRelationUpsertStrategyService.UpsertRequest){
        val child = upsertRequest.source
        val parent = upsertRequest.target

        val allOwningAncestors = getAllAncestors(upsertRequest)
        if(allOwningAncestors.contains(child))
            throw BpdmValidationException("Circular ownership detected in entity hierarchy: legal entity '${child.bpn}' is (transitively) owning '${parent.bpn}' and therefore can't be owned by '${parent.bpn}'.")
    }

    /**
     * Fetch the whole owning parent tree (ancestors, parents of parents) of the given legal entity
     */
    private fun getAllAncestors(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): Set<LegalEntityDb>{
        val parentLegalEntity = upsertRequest.target
        val allParents = mutableSetOf<LegalEntityDb>()
        val parentProcessingQueue =  ArrayDeque<LegalEntityDb>()

        parentProcessingQueue.addFirst(parentLegalEntity)

        lateinit var currentParent: LegalEntityDb

        do{
            currentParent = parentProcessingQueue.removeFirst()
            allParents.add(currentParent)

            // Fetch Relations in which the currently processed parent node is a child
            val parentParentRelations = relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsOwnedBy, currentParent)
            val overlappingParentParentRelations = relationUpsertService.filterOverlappingRelations(upsertRequest, parentParentRelations)

            val parentsOfCurrent = overlappingParentParentRelations.map { it.endNode }
            parentsOfCurrent.forEach { parentProcessingQueue.addFirst(it) }
        }while (parentProcessingQueue.isNotEmpty())

        return allParents
    }

}