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

import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.ReasonCodeRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.springframework.stereotype.Service
import org.eclipse.tractusx.orchestrator.api.model.RelationType as OrchestratorRelationType
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7

@Service
class TaskLegalEntityRelationsStepBuildService(
    private val alternativeHeadquarterRelationService: AlternativeHeadquarterRelationUpsertService,
    private val legalEntityRepository: LegalEntityRepository,
    private val managedRelationUpsertService: ManagedRelationUpsertService,
    private val ownedByRelationService: OwnedByRelationUpsertService,
    private val isReplacedByRelationService: IsReplacedByRelationUpsertService,
    private val relationRepository: RelationRepository,
    private val reasonCodeRepository: ReasonCodeRepository,
    private val relationValidityPeriodValidator: RelationValidityPeriodValidator
) {
    @Transactional
    fun upsertBusinessPartnerRelations(taskEntry: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto {
        val relationDto = taskEntry.businessPartnerRelations

        // Prevent self-referencing relations
        if (relationDto.businessPartnerSourceBpn == relationDto.businessPartnerTargetBpn) {
            throw BpdmValidationException("A legal entity cannot have a relation to itself (BPNL: ${relationDto.businessPartnerSourceBpn}).")
        }
        // Fetch legal entities by BPNL
        val sourceLegalEntity = legalEntityRepository.findByBpnIgnoreCase(relationDto.businessPartnerSourceBpn)
            ?: throw BpdmValidationException("Source legal entity with specified BPNL : ${relationDto.businessPartnerSourceBpn} not found")

        val targetLegalEntity = legalEntityRepository.findByBpnIgnoreCase(relationDto.businessPartnerTargetBpn)
            ?: throw BpdmValidationException("Target legal entity with specified BPNL : ${relationDto.businessPartnerTargetBpn} not found")

        val reasonCode  = relationDto.reasonCode?.let {
            reasonCodeRepository.findByTechnicalKey(it) ?: throw BpdmValidationException("Relation reason code '${relationDto.reasonCode}' not found")
        }

        relationValidityPeriodValidator.validate(relationDto)

        // Map states from orchestrator
        val validityPeriods = relationDto.validityPeriods.map {
            RelationValidityPeriodDb(
                validFrom = it.validFrom,
                validTo = it.validTo
            )
        }

        val legalEntityRelationType: LegalEntityRelationTypeV7 = when(relationDto.relationType){
            OrchestratorRelationType.IsAlternativeHeadquarterFor -> LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor
            OrchestratorRelationType.IsManagedBy -> LegalEntityRelationTypeV7.IsManagedBy
            OrchestratorRelationType.IsOwnedBy -> LegalEntityRelationTypeV7.IsOwnedBy
            OrchestratorRelationType.IsReplacedBy -> LegalEntityRelationTypeV7.IsReplacedBy
        }

        val existingRelation = relationRepository.findAll(
            RelationRepository.byRelation(
                startNode = sourceLegalEntity,
                endNode = targetLegalEntity,
                type = legalEntityRelationType
            )
        ).singleOrNull()

        val upsertRequest = IRelationUpsertStrategyService.UpsertRequest(
            source = sourceLegalEntity,
            target = targetLegalEntity,
            validityPeriods = validityPeriods,
            existingRelation = existingRelation,
            reasonCode = reasonCode
        )
        val strategyService : IRelationUpsertStrategyService = when(legalEntityRelationType){
            LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor -> alternativeHeadquarterRelationService
            LegalEntityRelationTypeV7.IsManagedBy -> managedRelationUpsertService
            LegalEntityRelationTypeV7.IsOwnedBy -> ownedByRelationService
            LegalEntityRelationTypeV7.IsReplacedBy -> isReplacedByRelationService
        }

        val upsertResult = strategyService.upsertRelation(upsertRequest)

        return TaskRelationsStepResultEntryDto(
            taskId = taskEntry.taskId,
            errors = emptyList(),
            businessPartnerRelations = upsertResult.value.toTaskDto()
        )
    }

    private fun RelationDb.toTaskDto(): BusinessPartnerRelations{
        return BusinessPartnerRelations(
            relationType = this.type.toTaskDto(),
            businessPartnerSourceBpn = this.startNode.bpn,
            businessPartnerTargetBpn = this.endNode.bpn,
            validityPeriods = this.validityPeriods.sortedBy { it.validFrom }.map {
                RelationValidityPeriod(
                    validFrom = it.validFrom,
                    validTo = it.validTo
                )
            },
            reasonCode = reasonCode?.technicalKey
        )
    }

    private fun LegalEntityRelationTypeV7.toTaskDto(): OrchestratorRelationType{
        return when(this){
            LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor -> OrchestratorRelationType.IsAlternativeHeadquarterFor
            LegalEntityRelationTypeV7.IsManagedBy -> OrchestratorRelationType.IsManagedBy
            LegalEntityRelationTypeV7.IsOwnedBy -> OrchestratorRelationType.IsOwnedBy
            LegalEntityRelationTypeV7.IsReplacedBy -> OrchestratorRelationType.IsReplacedBy
        }
    }
}