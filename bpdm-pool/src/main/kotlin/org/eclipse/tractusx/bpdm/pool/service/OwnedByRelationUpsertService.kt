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

import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.dto.result.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service

@Service
class OwnedByRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository
): IRelationUpsertStrategyService {

    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb> {
        val proposedSource = upsertRequest.source
        val proposedTarget = upsertRequest.target

        validateSingleParent(upsertRequest.source, upsertRequest.target)
        validateNoCycles(upsertRequest.source, upsertRequest.target)


        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(proposedSource, proposedTarget, RelationType.IsOwnedBy)
        )

        return result
    }


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