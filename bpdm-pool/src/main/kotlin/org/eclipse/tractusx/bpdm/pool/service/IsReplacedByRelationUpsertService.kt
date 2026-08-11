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
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service

/**
 * Writes succession between two legal entities, in which the source is replaced by the target.
 */
@Service
class IsReplacedByRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository
) : IRelationUpsertStrategyService {

    /**
     * Persists the succession and rejects it if the predecessor already has a different successor in an
     * overlapping validity period, or if it would close a replacement cycle over overlapping periods.
     */
    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb> {
        validateSingleSuccessor(upsertRequest)
        validateNoCycles(upsertRequest)

        return relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(
                source = upsertRequest.source,
                target = upsertRequest.target,
                legalEntityRelationType = LegalEntityRelationType.IsReplacedBy,
                validityPeriods = upsertRequest.validityPeriods,
                existingRelation = upsertRequest.existingRelation,
                reasonCode = upsertRequest.reasonCode
            )
        )
    }

    // Several predecessors may share one successor, so the mirror image of this check is deliberately absent: a merger
    // of multiple legal entities into one is a valid succession.
    private fun validateSingleSuccessor(upsertRequest: IRelationUpsertStrategyService.UpsertRequest) {
        val predecessor = upsertRequest.source
        val successor = upsertRequest.target

        val existingSuccessions = relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsReplacedBy, predecessor)
        val overlappingSuccessions = relationUpsertService.filterOverlappingRelations(upsertRequest, existingSuccessions)

        overlappingSuccessions.forEach { succession ->
            if (succession.endNode.bpn != successor.bpn)
                throw BpdmValidationException(
                    "Multiple successors assigned to the same legal entity: legal entity '${predecessor.bpn}' can't be replaced by " +
                            "'${successor.bpn}' as it is already replaced by '${succession.endNode.bpn}' in an overlapping validity period."
                )
        }
    }

    private fun validateNoCycles(upsertRequest: IRelationUpsertStrategyService.UpsertRequest) {
        val predecessor = upsertRequest.source
        val successor = upsertRequest.target

        if (getAllSuccessors(upsertRequest).contains(predecessor.bpn))
            throw BpdmValidationException(
                "Circular replacement detected: legal entity '${predecessor.bpn}' is (transitively) replacing '${successor.bpn}' " +
                        "and therefore can't be replaced by '${successor.bpn}'."
            )
    }

    private fun getAllSuccessors(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): Set<String> {
        val visitedBpnls = mutableSetOf<String>()
        val successorProcessingQueue = ArrayDeque<LegalEntityDb>()

        successorProcessingQueue.addFirst(upsertRequest.target)

        while (successorProcessingQueue.isNotEmpty()) {
            val currentSuccessor = successorProcessingQueue.removeFirst()

            if (!visitedBpnls.add(currentSuccessor.bpn))
                continue

            val successionsOfCurrent = relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsReplacedBy, currentSuccessor)
            relationUpsertService.filterOverlappingRelations(upsertRequest, successionsOfCurrent)
                .forEach { successorProcessingQueue.addFirst(it.endNode) }
        }

        return visitedBpnls
    }
}
