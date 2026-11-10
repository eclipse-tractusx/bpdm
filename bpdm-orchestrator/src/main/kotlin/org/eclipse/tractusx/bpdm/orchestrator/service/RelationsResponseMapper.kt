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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsGoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsTaskErrorDb
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.BusinessPartnerRelationsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.StepStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskClientRelationsStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskProcessingRelationsStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsErrorV7Dto
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RelationsResponseMapper {

    fun toClientState(task: RelationsGoldenRecordTaskDb, timeout: Instant) =
        with(task) {
            TaskClientRelationsStateV7Dto(
                taskId = task.uuid.toString(),
                recordId = task.gateRecord.privateId.toString(),
                businessPartnerRelationsResult = toBusinessPartnerRelationsResult(businessPartnerRelations),
                processingState = toProcessingState(task, timeout)
            )
        }

    fun toBusinessPartnerRelationsResult(businessPartnerRelations: RelationsGoldenRecordTaskDb.BusinessPartnerRelations) =
        with(businessPartnerRelations) {
            BusinessPartnerRelationsV7Dto(
                relationType = toRelationType(relationType),
                businessPartnerSourceBpn = businessPartnerSourceBpn,
                businessPartnerTargetBpn = businessPartnerTargetBpn,
                validityPeriods = businessPartnerRelations.validityPeriods.map {
                    RelationValidityPeriodV7Dto(
                        validFrom = it.validFrom,
                        validTo = it.validTo
                    )
                }.toMutableList(),
                reasonCode = reasonCode
            )
        }

    fun toProcessingState(task: RelationsGoldenRecordTaskDb, timeout: Instant) =
        with(task.processingState) {
            TaskProcessingRelationsStateV7Dto(
                resultState = toResultState(resultState),
                step = step,
                stepState = toStepState(stepState),
                errors = errors.map { toTaskError(it) },
                createdAt = task.createdAt.instant,
                modifiedAt = task.updatedAt.instant,
                timeout = timeout
            )
        }

    fun toRelationType(relationType: RelationsGoldenRecordTaskDb.RelationType) =
        when(relationType){
            RelationsGoldenRecordTaskDb.RelationType.IsAlternativeHeadquarterFor -> RelationTypeV7.IsAlternativeHeadquarterFor
            RelationsGoldenRecordTaskDb.RelationType.IsManagedBy -> RelationTypeV7.IsManagedBy
            RelationsGoldenRecordTaskDb.RelationType.IsOwnedBy -> RelationTypeV7.IsOwnedBy
            RelationsGoldenRecordTaskDb.RelationType.IsReplacedBy -> RelationTypeV7.IsReplacedBy
        }

    fun toResultState(resultState: RelationsGoldenRecordTaskDb.ResultState) =
        when(resultState){
            RelationsGoldenRecordTaskDb.ResultState.Pending -> ResultStateV7.Pending
            RelationsGoldenRecordTaskDb.ResultState.Success ->  ResultStateV7.Success
            RelationsGoldenRecordTaskDb.ResultState.Error ->  ResultStateV7.Error
            RelationsGoldenRecordTaskDb.ResultState.Aborted ->  ResultStateV7.Error
        }

    fun toStepState(stepState: RelationsGoldenRecordTaskDb.StepState) =
        when(stepState){
            RelationsGoldenRecordTaskDb.StepState.Queued -> StepStateV7.Queued
            RelationsGoldenRecordTaskDb.StepState.Reserved -> StepStateV7.Reserved
            RelationsGoldenRecordTaskDb.StepState.Success -> StepStateV7.Success
            RelationsGoldenRecordTaskDb.StepState.Error -> StepStateV7.Error
            RelationsGoldenRecordTaskDb.StepState.Aborted -> StepStateV7.Error
        }

    fun toTaskError(taskError: RelationsTaskErrorDb) =
        with(taskError) {
            TaskRelationsErrorV7Dto(type = type, description = description)
        }
}