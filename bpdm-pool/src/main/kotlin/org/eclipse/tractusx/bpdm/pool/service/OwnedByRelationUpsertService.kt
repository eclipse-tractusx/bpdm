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
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class OwnedByRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository
): IRelationUpsertStrategyService {

    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb> {

        validateSingleOwner(upsertRequest)
        validateNoCycles(upsertRequest)

        val upsertServiceRequest = RelationUpsertService.UpsertRequest(upsertRequest.source, upsertRequest.target, RelationType.IsOwnedBy, upsertRequest.states)
        return relationUpsertService.upsertRelation(upsertServiceRequest)
    }

    /**
     * Checks that the proposed source (child) does not already have another parent
     * with overlapping validity periods.
     */
    private fun validateSingleOwner(upsertRequest: IRelationUpsertStrategyService.UpsertRequest) {
        val allChildRelations = relationRepository.findByTypeAndStartNode(RelationType.IsOwnedBy, upsertRequest.source)
        val allOverlappingChildRelations = relationUpsertService.filterOverlappingRelations(upsertRequest, allChildRelations)

        allOverlappingChildRelations.forEach { relation ->
            if (relation.endNode != upsertRequest.target) {
                throw BpdmValidationException(
                    "Multiple owning entities assigned to the same owned entity during overlapping validity: " +
                            "legal entity '${upsertRequest.source.bpn}' can't be owned by '${upsertRequest.target.bpn}' " +
                            "as it is already owned by '${relation.endNode.bpn}' in overlapping period."
                )
            }
        }
    }

    /**
     * Checks that no cycles exist in the ownership hierarchy.
     */
    private fun validateNoCycles(upsertRequest: IRelationUpsertStrategyService.UpsertRequest) {
        val allOwningAncestors = getAllOverlappingAncestors(upsertRequest)
        if (allOwningAncestors.contains(upsertRequest.source)) {
            throw BpdmValidationException(
                "Circular ownership detected: legal entity '${upsertRequest.source.bpn}' is (transitively) owning '${upsertRequest.target.bpn}' and therefore can't be owned by it."
            )
        }
    }

    /**
     * Fetch the whole overlapping, owning parent tree (ancestors, parents of parents) of the given upsert request
     */
    private fun getAllOverlappingAncestors(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): Set<LegalEntityDb>{
        val allOwners = mutableSetOf<LegalEntityDb>()
        val ownerProcessingQueue =  ArrayDeque<LegalEntityDb>()

        ownerProcessingQueue.addFirst(upsertRequest.target)

        lateinit var currentOwner: LegalEntityDb

        do{
            currentOwner = ownerProcessingQueue.removeFirst()
            allOwners.add(currentOwner)
            // Fetch Relations in which the currently processed parent node is a child
            val ownerOfOwnerRelations = relationRepository.findByTypeAndStartNode(RelationType.IsOwnedBy, currentOwner)
            val overlappingOwnerOfOwnerRelations = relationUpsertService.filterOverlappingRelations(upsertRequest, ownerOfOwnerRelations)
            val ownersOfCurrent = overlappingOwnerOfOwnerRelations.map { it.endNode }
            ownersOfCurrent.forEach { ownerProcessingQueue.addFirst(it) }
        }while (ownerProcessingQueue.isNotEmpty())

        return allOwners
    }
}