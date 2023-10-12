/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.IBaseLegalEntityDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataDto
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityState
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.scheduling.annotation.Scheduled
import java.time.Instant
import java.time.temporal.ChronoUnit

class FetchAndReserveCleaningStepService(
    private val orchestrationClient: OrchestrationApiClient,
    private val metadataService: MetadataService,
    private val requestValidationService: RequestValidationService,
    private val bpnIssuingService: BpnIssuingService,
) {
    private val logger = KotlinLogging.logger { }

    @Scheduled(cron = "\${bpdm.opensearch.export-scheduler-cron-expr:-}", zone = "UTC")
    fun fetchAndReserve() {

        val reservationRequest = TaskStepReservationRequest(step = TaskStep.PoolSync, amount = 10)
        val taskStepReservation = orchestrationClient.goldenRecordTasks.reserveTasksForStep(reservationRequest = reservationRequest)

        val taskResults = upsertGoldenRecordIntoPool(taskStepReservation.reservedTasks)
        orchestrationClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(step=TaskStep.PoolSync,results = taskResults))
    }

    private fun upsertGoldenRecordIntoPool(tasks: List<TaskStepReservationEntryDto>): List<TaskStepResultEntryDto> {

        val isTaskCreateLegalEntity =
            { task: TaskStepReservationEntryDto -> task.businessPartner.legalEntity?.bpnLReference?.referenceType == BpnReferenceType.BpnRequestIdentifier }

        val legalEntitiesToCreateSteps: List<TaskStepReservationEntryDto> = tasks
            .filter { isTaskCreateLegalEntity(it) }

        val legalEntityByTask = legalEntitiesToCreateSteps
            .associateWith { it.businessPartner.legalEntity as IBaseLegalEntityDto }

        val addressByTask = legalEntitiesToCreateSteps
            .associateWith { it.businessPartner.legalEntity?.legalAddress as LogisticAddressDto }


        val errorsByRequest = requestValidationService.validateLegalEntityCreatesOrchestrator(legalEntityByTask)
        val errorsByRequestAddress = requestValidationService.validateLegalEntityCreatesAddressesOrchestrator(addressByTask)

        val legalEntityCreateTaskResults = legalEntitiesToCreateSteps
            .map { taskStep ->
                if (errorsByRequest.containsKey(taskStep) || errorsByRequestAddress.containsKey(taskStep)) {
                    taskResultsForErrors(
                        taskStep.taskId,
                        errorsByRequest.getOrDefault(taskStep, emptyList()) + errorsByRequestAddress.getOrDefault(taskStep, emptyList())
                    )
                } else {
                    legalEntityCreateTaskResult(taskStep)
                }
            }
        return legalEntityCreateTaskResults
    }

    private fun taskResultsForErrors(errorTaskId: String, errors: Collection<ErrorInfo<LegalEntityCreateError>>): TaskStepResultEntryDto {

        return TaskStepResultEntryDto(taskId = errorTaskId, errors = errors.map { TaskErrorDto(type = TaskErrorType.Unspecified, description = it.message) })
    }

    private fun legalEntityCreateTaskResult(task: TaskStepReservationEntryDto): TaskStepResultEntryDto {

        val legalEntity = task.businessPartner.legalEntity
        val legalName = legalEntity?.legalName
        val legalAddress = legalEntity?.legalAddress

        return if (legalEntity != null && legalName != null && legalAddress != null) {
            val bpnLs = bpnIssuingService.issueLegalEntityBpns(1)
            val legalEntityMetadataMap = metadataService.getMetadata(listOf(legalEntity)).toMapping()
            val newLegalEntity = createLegalEntity(legalEntity, bpnLs[0], legalName, legalEntityMetadataMap)
            // todo create address, changelogService and write to db
            TaskStepResultEntryDto(
                taskId = task.taskId,
                businessPartner = BusinessPartnerFullDto(generic = task.businessPartner.generic)
            )
        } else {
            TaskStepResultEntryDto(
                taskId = task.taskId,
                errors = listOf(
                    TaskErrorDto(
                        type = TaskErrorType.Unspecified,
                        description = "Legal name or legal address is empty"
                    )
                )
            )
        }
    }


    private fun createLegalEntity(
        legalEntityDto: LegalEntityDto,
        bpnL: String,
        legalNameValue: String,
        metadataMap: BusinessPartnerBuildService.LegalEntityMetadataMapping
    ): LegalEntity {

        // it has to be validated that the legalForm exits
        val legalForm = legalEntityDto.legalForm?.let { metadataMap.legalForms[it]!! }
        val legalName = Name(
            value = legalNameValue,
            shortName = legalEntityDto.legalShortName
        )
        val newLegalEntity = LegalEntity(
            bpn = bpnL,
            legalName = legalName,
            legalForm = legalForm,
            currentness = Instant.now().truncatedTo(ChronoUnit.MICROS),
        )
        updateLegalEntity(newLegalEntity, legalEntityDto, legalName, legalEntityDto.identifiers.map { toEntity(it, metadataMap.idTypes, newLegalEntity) })

        return newLegalEntity
    }

    private fun updateLegalEntity(
        legalEntity: LegalEntity,
        request: LegalEntityDto,
        legalName: Name,
        identifiers: List<LegalEntityIdentifier>
    ) {

        legalEntity.currentness = createCurrentnessTimestamp()

        legalEntity.legalName = legalName

        legalEntity.identifiers.clear()
        legalEntity.states.clear()
        legalEntity.classifications.clear()

        legalEntity.states.addAll(request.states.map { toEntity(it, legalEntity) })
        legalEntity.identifiers.addAll(identifiers)
        legalEntity.classifications.addAll(request.classifications.map { toEntity(it, legalEntity) }.toSet())
    }

    private fun createCurrentnessTimestamp(): Instant {
        return Instant.now().truncatedTo(ChronoUnit.MICROS)
    }

    private fun toEntity(dto: org.eclipse.tractusx.orchestrator.api.model.LegalEntityState, legalEntity: LegalEntity): LegalEntityState {
        return LegalEntityState(
            description = dto.description,
            validFrom = dto.validFrom,
            validTo = dto.validTo,
            type = dto.type,
            legalEntity = legalEntity
        )
    }

    private fun toEntity(
        dto: LegalEntityIdentifierDto,
        idTypes: Map<String, IdentifierType>,
        partner: LegalEntity
    ): LegalEntityIdentifier {
        return LegalEntityIdentifier(
            value = dto.value,
            type = idTypes[dto.type]!!,
            issuingBody = dto.issuingBody,
            legalEntity = partner
        )
    }

    private fun toEntity(dto: BusinessPartnerClassificationDto, partner: LegalEntity): LegalEntityClassification {
        return LegalEntityClassification(
            value = dto.value,
            code = dto.code,
            type = dto.type,
            legalEntity = partner
        )
    }

    private fun LegalEntityMetadataDto.toMapping() =
        BusinessPartnerBuildService.LegalEntityMetadataMapping(
            idTypes = idTypes.associateBy { it.technicalKey },
            legalForms = legalForms.associateBy { it.technicalKey }
        )
}