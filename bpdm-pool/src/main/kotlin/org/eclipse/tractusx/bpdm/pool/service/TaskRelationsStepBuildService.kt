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

import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.springframework.stereotype.Service
import org.eclipse.tractusx.orchestrator.api.model.RelationType as OrchestratorRelationType

@Service
class TaskRelationsStepBuildService(
    private val alternativeHeadquarterRelationService: AlternativeHeadquarterRelationUpsertService,
    private val legalEntityRepository: LegalEntityRepository,
    private val managedRelationUpsertService: ManagedRelationUpsertService,
    private val ownedByRelationService: OwnedByRelationUpsertService
) {

    @Transactional
    fun upsertBusinessPartnerRelations(taskEntry: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto {
        val relationDto = taskEntry.businessPartnerRelations

        // Prevent self-referencing relations
        if (relationDto.businessPartnerSourceBpnl == relationDto.businessPartnerTargetBpnl) {
            throw BpdmValidationException("A legal entity cannot have a relation to itself (BPNL: ${relationDto.businessPartnerSourceBpnl}).")
        }

        // Fetch legal entities by BPNL
        val sourceLegalEntity = legalEntityRepository.findByBpnIgnoreCase(relationDto.businessPartnerSourceBpnl)
            ?: throw BpdmValidationException("Source legal entity with specified BPNL : ${relationDto.businessPartnerSourceBpnl} not found")

        val targetLegalEntity = legalEntityRepository.findByBpnIgnoreCase(relationDto.businessPartnerTargetBpnl)
            ?: throw BpdmValidationException("Target legal entity with specified BPNL : ${relationDto.businessPartnerTargetBpnl} not found")

        val upsertRequest = IRelationUpsertStrategyService.UpsertRequest(sourceLegalEntity, targetLegalEntity)
        val strategyService : IRelationUpsertStrategyService = when(relationDto.relationType){
            OrchestratorRelationType.IsAlternativeHeadquarterFor -> alternativeHeadquarterRelationService
            OrchestratorRelationType.IsManagedBy -> managedRelationUpsertService
            OrchestratorRelationType.IsOwnedBy -> ownedByRelationService
        }

        val upsertResult = strategyService.upsertRelation(upsertRequest)

        return TaskRelationsStepResultEntryDto(
            taskId = taskEntry.taskId,
            errors = emptyList(),
            businessPartnerRelations = upsertResult.record.toTaskDto()
        )
    }

    private fun RelationDb.toTaskDto(): BusinessPartnerRelations{
        return BusinessPartnerRelations(
            type.toTaskDto(),
            startNode.bpn,
            endNode.bpn
        )
    }

    private fun RelationType.toTaskDto(): OrchestratorRelationType{
        return when(this){
            RelationType.IsAlternativeHeadquarterFor -> OrchestratorRelationType.IsAlternativeHeadquarterFor
            RelationType.IsManagedBy -> OrchestratorRelationType.IsManagedBy
            RelationType.IsOwnedBy -> OrchestratorRelationType.IsOwnedBy
        }
    }
}