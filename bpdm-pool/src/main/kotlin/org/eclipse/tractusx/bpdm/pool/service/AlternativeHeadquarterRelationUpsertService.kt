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
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime

@Service
class AlternativeHeadquarterRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository
): IRelationUpsertStrategyService {

    @Transactional
    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb>{
        val standardisedRequest = standardise(upsertRequest)

        val computedStates = upsertRequest.states.map { dto ->
            RelationStateDb(
                validFrom = dto.validFrom,
                validTo = dto.validTo,
                type = if ( Instant.now() in dto.validFrom..dto.validTo) BusinessStateType.ACTIVE else BusinessStateType.INACTIVE
            )
        }

        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(standardisedRequest.source, standardisedRequest.target, RelationType.IsAlternativeHeadquarterFor, computedStates)
        )

        if(result.upsertType == UpsertType.Created){
            createTransitiveRelations(standardisedRequest)
        }

        return result
    }

    private fun standardise(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): IRelationUpsertStrategyService.UpsertRequest{
        val proposedSource = upsertRequest.source
        val proposedTarget = upsertRequest.target

        val computedStates = upsertRequest.states.map { dto ->
            RelationStateDb(
                validFrom = dto.validFrom,
                validTo = dto.validTo,
                type = dto.type
            )
        }

        val sourceIsOlder = proposedSource.createdAt < proposedTarget.createdAt
        val upsertRequest = if(sourceIsOlder){
            IRelationUpsertStrategyService.UpsertRequest(source = proposedTarget, target = proposedSource, computedStates)
        }else{
            IRelationUpsertStrategyService.UpsertRequest(source = proposedSource, target = proposedTarget, computedStates)
        }

        return upsertRequest
    }

    private fun createTransitiveRelations(upsertRequest: IRelationUpsertStrategyService.UpsertRequest){
        val transitiveSourceRelations = relationRepository.findInSourceOrTarget(RelationType.IsAlternativeHeadquarterFor, upsertRequest.source)
        val transitiveTargetRelations = relationRepository.findInSourceOrTarget(RelationType.IsAlternativeHeadquarterFor, upsertRequest.target)

        val transitiveSourceRequests = createTransitiveUpsertRequests(upsertRequest.target, transitiveSourceRelations, upsertRequest.states)
        val transitiveTargetRequests = createTransitiveUpsertRequests(upsertRequest.source, transitiveTargetRelations, upsertRequest.states)


        transitiveSourceRequests
            .plus(transitiveTargetRequests)
            .filter { it.source.id != upsertRequest.source.id || it.target.id != upsertRequest.target.id }
            .distinctBy { Pair(it.source.id, it.target.id) }
            .map { RelationUpsertService.UpsertRequest(it.source, it.target, RelationType.IsAlternativeHeadquarterFor, it.states) }
            .forEach { relationUpsertService.upsertRelation(it) }
    }

    private fun createTransitiveUpsertRequests(
        legalEntity: LegalEntityDb,
        relations: Set<RelationDb>,
        originalStates: Collection<RelationStateDb>
    ): List<IRelationUpsertStrategyService.UpsertRequest>{
        val allLegalEntities = relations.flatMap { listOf(it.startNode, it.endNode) }.toSet()

        return  allLegalEntities
            .filter { legalEntity.id != it.id }
            .map { IRelationUpsertStrategyService.UpsertRequest(legalEntity, it, originalStates) }
            .map { standardise(it) }
            .distinctBy { Pair(it.source.id, it.target.id) }
    }

}