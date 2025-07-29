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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsGoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsTaskErrorDb
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RelationsResponseMapper {

    fun toClientState(task: RelationsGoldenRecordTaskDb, timeout: Instant) =
        with(task) {
            TaskClientRelationsStateDto(
                taskId = task.uuid.toString(),
                recordId = task.gateRecord.privateId.toString(),
                businessPartnerRelationsResult = toBusinessPartneRelationsResult(businessPartnerRelations),
                processingState = toProcessingState(task, timeout)
            )
        }

    fun toBusinessPartneRelationsResult(businessPartnerRelations: RelationsGoldenRecordTaskDb.BusinessPartnerRelations) =
        with(businessPartnerRelations) {
            BusinessPartnerRelations(
                relationType = toRelationType(relationType),
                businessPartnerSourceBpnl = businessPartnerSourceBpnl,
                businessPartnerTargetBpnl = businessPartnerTargetBpnl,
                validFrom = validFrom,
                validTo = validTo,
            )
        }

    fun toProcessingState(task: RelationsGoldenRecordTaskDb, timeout: Instant) =
        with(task.processingState) {
            TaskProcessingRelationsStateDto(
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
            RelationsGoldenRecordTaskDb.RelationType.IsAlternativeHeadquarterFor -> RelationType.IsAlternativeHeadquarterFor
            RelationsGoldenRecordTaskDb.RelationType.IsManagedBy -> RelationType.IsManagedBy
        }

    fun toResultState(resultState: RelationsGoldenRecordTaskDb.ResultState) =
        when(resultState){
            RelationsGoldenRecordTaskDb.ResultState.Pending -> ResultState.Pending
            RelationsGoldenRecordTaskDb.ResultState.Success ->  ResultState.Success
            RelationsGoldenRecordTaskDb.ResultState.Error ->  ResultState.Error
            RelationsGoldenRecordTaskDb.ResultState.Aborted ->  ResultState.Error
        }

    fun toStepState(stepState: RelationsGoldenRecordTaskDb.StepState) =
        when(stepState){
            RelationsGoldenRecordTaskDb.StepState.Queued -> StepState.Queued
            RelationsGoldenRecordTaskDb.StepState.Reserved -> StepState.Reserved
            RelationsGoldenRecordTaskDb.StepState.Success -> StepState.Success
            RelationsGoldenRecordTaskDb.StepState.Error -> StepState.Error
            RelationsGoldenRecordTaskDb.StepState.Aborted -> StepState.Error
        }

    fun toTaskError(taskError: RelationsTaskErrorDb) =
        with(taskError) {
            TaskRelationsErrorDto(type = type, description = description)
        }

    fun toBusinessPartneRelationVerboseResult(businessPartnerRelations: RelationsGoldenRecordTaskDb.BusinessPartnerRelations) =
        with(businessPartnerRelations) {
            BusinessPartnerRelationVerboseDto(
                relationType = toRelationType(relationType),
                businessPartnerSourceBpnl = businessPartnerSourceBpnl,
                businessPartnerTargetBpnl = businessPartnerTargetBpnl,
                validFrom = validFrom,
                validTo = validTo,
                isActive = Instant.now() in validFrom..validTo
            )
        }
}