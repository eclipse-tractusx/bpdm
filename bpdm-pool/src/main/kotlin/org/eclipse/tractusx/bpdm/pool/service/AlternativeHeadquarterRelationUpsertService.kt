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

import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlternativeHeadquarterRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository
): IRelationUpsertStrategyService {

    @Transactional
    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb>{
        val standardisedRequest = standardise(upsertRequest)

        validateOnlyOneAlternative(standardisedRequest)

        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(standardisedRequest.source, standardisedRequest.target, RelationType.IsAlternativeHeadquarterFor, standardisedRequest.validityPeriods)
        )

        return result
    }

    private fun standardise(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): IRelationUpsertStrategyService.UpsertRequest{
        val proposedSource = upsertRequest.source
        val proposedTarget = upsertRequest.target

        val sourceIsOlder = proposedSource.createdAt < proposedTarget.createdAt
        val upsertRequest = if(sourceIsOlder){
            IRelationUpsertStrategyService.UpsertRequest(source = proposedTarget, target = proposedSource, validityPeriods = upsertRequest.validityPeriods)
        }else{
            IRelationUpsertStrategyService.UpsertRequest(source = proposedSource, target = proposedTarget,  validityPeriods = upsertRequest.validityPeriods)
        }

        return upsertRequest
    }

    private fun validateOnlyOneAlternative(upsertRequest: IRelationUpsertStrategyService.UpsertRequest){
        validateOnlyOneAlternative(upsertRequest.source, upsertRequest)
        validateOnlyOneAlternative(upsertRequest.target, upsertRequest)
    }

    private fun validateOnlyOneAlternative(legalEntity: LegalEntityDb, upsertRequest: IRelationUpsertStrategyService.UpsertRequest){
        val relations =  relationRepository.findInSourceOrTarget(RelationType.IsAlternativeHeadquarterFor, legalEntity)
        val overlappingRelations = relationUpsertService.filterOverlappingRelations(upsertRequest, relations)

        val otherRelations =  overlappingRelations.filterNot { it.startNode.bpn == upsertRequest.source.bpn && it.endNode.bpn == upsertRequest.target.bpn }

        if(otherRelations.isNotEmpty()){
            val otherRelation = otherRelations.first()
            val otherLegalEntity = if(otherRelation.startNode.bpn == legalEntity.bpn) otherRelation.endNode else otherRelation.startNode
            throw BpdmValidationException(
                "Invalid 'IsAlternativeHeadquarter' relation: Legal Entity ${legalEntity.bpn} is already alternative headquarter to ${otherLegalEntity.bpn}"
            )
        }
    }
}