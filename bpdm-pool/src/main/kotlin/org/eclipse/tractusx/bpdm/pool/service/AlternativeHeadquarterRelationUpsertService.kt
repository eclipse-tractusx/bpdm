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
import org.eclipse.tractusx.orchestrator.api.model.RelationStateDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AlternativeHeadquarterRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository
): IRelationUpsertStrategyService {

    companion object{
        private val alwaysActiveState = RelationStateDb(
            validFrom = Instant.parse("1970-01-01T00:00:00Z"),
            validTo = Instant.parse("9999-12-31T23:59:59Z"),
            type = BusinessStateType.ACTIVE
        )
    }

    @Transactional
    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb>{
        if(!isAlwaysActive(upsertRequest)) throw BpdmValidationException("Invalid 'IsAlternativeHeadquarter' relation: This relation type does not support any validity constraints.")

        val standardisedRequest = standardise(upsertRequest)

        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(standardisedRequest.source, standardisedRequest.target, RelationType.IsAlternativeHeadquarterFor, standardisedRequest.states)
        )

        if(result.upsertType == UpsertType.Created){
            createTransitiveRelations(standardisedRequest)
        }

        return result
    }

    private fun standardise(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): IRelationUpsertStrategyService.UpsertRequest{
        val proposedSource = upsertRequest.source
        val proposedTarget = upsertRequest.target

        val sourceIsOlder = proposedSource.createdAt < proposedTarget.createdAt
        val upsertRequest = if(sourceIsOlder){
            IRelationUpsertStrategyService.UpsertRequest(source = proposedTarget, target = proposedSource, states = upsertRequest.states)
        }else{
            IRelationUpsertStrategyService.UpsertRequest(source = proposedSource, target = proposedTarget,  states = upsertRequest.states)
        }

        return upsertRequest
    }

    private fun createTransitiveRelations(upsertRequest: IRelationUpsertStrategyService.UpsertRequest){
        val transitiveSourceRelations = relationRepository.findInSourceOrTarget(RelationType.IsAlternativeHeadquarterFor, upsertRequest.source)
        val transitiveTargetRelations = relationRepository.findInSourceOrTarget(RelationType.IsAlternativeHeadquarterFor, upsertRequest.target)

        val transitiveSourceRequests = createTransitiveUpsertRequests(upsertRequest.target, upsertRequest.states, transitiveSourceRelations)
        val transitiveTargetRequests = createTransitiveUpsertRequests(upsertRequest.source, upsertRequest.states, transitiveTargetRelations)

        transitiveSourceRequests
            .plus(transitiveTargetRequests)
            .filter { it.source.id != upsertRequest.source.id || it.target.id != upsertRequest.target.id }
            .distinctBy { Pair(it.source.id, it.target.id) }
            .map { RelationUpsertService.UpsertRequest(it.source, it.target, RelationType.IsAlternativeHeadquarterFor, it.states) }
            .forEach { relationUpsertService.upsertRelation(it) }
    }

    private fun createTransitiveUpsertRequests(legalEntity: LegalEntityDb, states: Collection<RelationStateDb>, relations: Set<RelationDb>): List<IRelationUpsertStrategyService.UpsertRequest>{
        val allLegalEntities = relations.flatMap { listOf(it.startNode, it.endNode) }.toSet()

        return  allLegalEntities
            .filter { legalEntity.id != it.id }
            .map { IRelationUpsertStrategyService.UpsertRequest(legalEntity, it, states) }
            .map { standardise(it) }
            .distinctBy { Pair(it.source.id, it.target.id) }
    }

    private fun isAlwaysActive(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): Boolean{
        return upsertRequest.states.singleOrNull()?.let { it == alwaysActiveState } ?: false
    }

}