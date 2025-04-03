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
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.springframework.stereotype.Service

@Service
class TaskRelationsStepBuildService(
    private val relationRepository: RelationRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService
) {

    @Transactional
    fun upsertBusinessPartnerRelations(taskEntry: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto {
        val relationDto = taskEntry.businessPartnerRelations

        // Fetch legal entities by BPNL
        val sourceLegalEntity = legalEntityRepository.findByBpnIgnoreCase(relationDto.businessPartnerSourceBpnl)
            ?: throw BpdmValidationException("Source legal entity with specified BPNL : ${relationDto.businessPartnerSourceBpnl} not found")

        val targetLegalEntity = legalEntityRepository.findByBpnIgnoreCase(relationDto.businessPartnerTargetBpnl)
            ?: throw BpdmValidationException("Target legal entity with specified BPNL : ${relationDto.businessPartnerTargetBpnl} not found")


        // Prevent self-referencing relations
        if (sourceLegalEntity == targetLegalEntity) {
            throw BpdmValidationException("A legal entity cannot have a relation to itself (BPNL: ${relationDto.businessPartnerSourceBpnl}).")
        }

        val existingRelation = relationRepository.findOne(
            RelationRepository.byRelation(
                startNode = sourceLegalEntity,
                endNode = targetLegalEntity,
                type = RelationType.valueOf(relationDto.relationType.name)
            )
        )

        return if (existingRelation.isPresent) {
            // Relation already exists, return existing details
            TaskRelationsStepResultEntryDto(
                taskId = taskEntry.taskId,
                errors = emptyList(),
                businessPartnerRelations = relationDto
            )
        } else {
            // Create new relation
            val newRelation = RelationDb(
                type = RelationType.valueOf(relationDto.relationType.name),
                startNode = sourceLegalEntity,
                endNode = targetLegalEntity,
                isActive = true
            )

            relationRepository.save(newRelation)

            changelogService.createChangelogEntry(ChangelogEntryCreateRequest(sourceLegalEntity.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY))
            changelogService.createChangelogEntry(ChangelogEntryCreateRequest(targetLegalEntity.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY))

            TaskRelationsStepResultEntryDto(
                taskId = taskEntry.taskId,
                errors = emptyList(),
                businessPartnerRelations = relationDto
            )
        }
    }
}