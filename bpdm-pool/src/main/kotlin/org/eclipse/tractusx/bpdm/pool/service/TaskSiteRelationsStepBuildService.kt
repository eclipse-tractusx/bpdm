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
import org.eclipse.tractusx.bpdm.pool.api.model.SiteRelationType
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.ReasonCodeRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRelationRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.springframework.stereotype.Service
import org.eclipse.tractusx.orchestrator.api.model.RelationType as OrchestratorRelationType

/**
 * Turns a reserved golden record relation task between two sites into the site relation the Pool stores for it.
 */
@Service
class TaskSiteRelationsStepBuildService(
    private val siteRepository: SiteRepository,
    private val siteRelationRepository: SiteRelationRepository,
    private val siteRelationUpsertService: SiteRelationUpsertService,
    private val reasonCodeRepository: ReasonCodeRepository,
    private val relationValidityPeriodValidator: RelationValidityPeriodValidator
) {

    /**
     * Writes the task's site relation and reports it back as the task result.
     */
    @Transactional
    fun upsertSiteRelations(taskEntry: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto {
        val siteRelationDto = taskEntry.businessPartnerRelations

        val sourceSite = siteRepository.findByBpn(siteRelationDto.businessPartnerSourceBpn)
            ?: throw BpdmValidationException("Source site BPNS ${siteRelationDto.businessPartnerSourceBpn} not found")

        val targetSite = siteRepository.findByBpn(siteRelationDto.businessPartnerTargetBpn)
            ?: throw BpdmValidationException("Target site BPNS ${siteRelationDto.businessPartnerTargetBpn} not found")

        val reasonCode = siteRelationDto.reasonCode?.let {
            reasonCodeRepository.findByTechnicalKey(it) ?: throw BpdmValidationException("Relation reason code '${siteRelationDto.reasonCode}' not found")
        }

        relationValidityPeriodValidator.validate(siteRelationDto)

        val siteRelationType = when (siteRelationDto.relationType) {
            OrchestratorRelationType.IsReplacedBy -> SiteRelationType.IsReplacedBy
            else -> throw BpdmValidationException("Unsupported site relation type: ${siteRelationDto.relationType}")
        }

        val existingRelation = siteRelationRepository.findAll(
            SiteRelationRepository.byRelation(
                startSite = sourceSite,
                endSite = targetSite,
                type = siteRelationType
            )
        ).singleOrNull()

        val upsertResult = siteRelationUpsertService.upsertRelation(
            SiteRelationUpsertService.UpsertRequest(
                source = sourceSite,
                target = targetSite,
                validityPeriods = siteRelationDto.validityPeriods.map { RelationValidityPeriodDb(it.validFrom, it.validTo) },
                existingRelation = existingRelation,
                reasonCode = reasonCode
            )
        )

        return TaskRelationsStepResultEntryDto(
            taskId = taskEntry.taskId,
            errors = emptyList(),
            businessPartnerRelations = upsertResult.value.toTaskDto()
        )
    }

    private fun SiteRelationDb.toTaskDto(): BusinessPartnerRelations =
        BusinessPartnerRelations(
            relationType = type.toTaskDto(),
            businessPartnerSourceBpn = startSite.bpn,
            businessPartnerTargetBpn = endSite.bpn,
            validityPeriods = validityPeriods.sortedBy { it.validFrom }.map { RelationValidityPeriod(validFrom = it.validFrom, validTo = it.validTo) },
            reasonCode = reasonCode?.technicalKey
        )

    private fun SiteRelationType.toTaskDto(): OrchestratorRelationType =
        when (this) {
            SiteRelationType.IsReplacedBy -> OrchestratorRelationType.IsReplacedBy
        }
}
