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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsGoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.SharingMemberRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.mapper.BusinessPartnerRelationsRequestMapper
import org.eclipse.tractusx.bpdm.orchestrator.repository.RelationsGoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.SharingMemberRecordRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.fetchRelationsData
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class RelationsGoldenRecordTaskService(
    private val relationsGoldenRecordTaskStateMachine: RelationsGoldenRecordTaskStateMachine,
    private val taskConfigProperties: TaskConfigProperties,
    private val relationsResponseMapper: RelationsResponseMapper,
    private val relationsTaskRepository: RelationsGoldenRecordTaskRepository,
    private val sharingMemberRecordRepository: SharingMemberRecordRepository,
    private val businessPartnerRelationsRequestMapper: BusinessPartnerRelationsRequestMapper
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createTasks(createRequest: TaskCreateRelationsRequest): TaskCreateRelationsResponse {
        logger.debug { "Creation of new relations golden record tasks: executing createTasks() with parameters $createRequest" }

        val gateRecords = getOrCreateGateRecords(createRequest.requests)
        abortOutdatedTasks(gateRecords.toSet())

        val createdTasks = createRequest.requests.zip(gateRecords)
            .map { (request, record) -> relationsGoldenRecordTaskStateMachine.initTask(createRequest.mode, businessPartnerRelationsRequestMapper.toBusinessPartnerRelationsRequest(request.businessPartnerRelations), record) }

        if (createdTasks.isNotEmpty())
            logger.info { "Created ${createdTasks.size} relation golden record tasks in mode ${createRequest.mode}: ${createdTasks.toLogIdentifiers()}" }

        return createdTasks
            .map { task -> relationsResponseMapper.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskCreateRelationsResponse(createdTasks = it) }
    }

    fun searchTaskStates(stateRequest: TaskStateRequest): TaskRelationsStateResponse {
        logger.debug { "Search for the state of relations golden record task: executing searchTaskStates() with parameters $stateRequest" }
        val requestsByTaskId = stateRequest.entries.associateBy { it.taskId }

        return stateRequest.entries.map { toUUID(it.taskId) }
            .let { uuids -> relationsTaskRepository.findByUuidIn(uuids.toSet()) }
            .also { tasks -> relationsTaskRepository.fetchRelationsData(tasks) }
            .filter { task -> requestsByTaskId[task.uuid.toString()]?.recordId == task.gateRecord.privateId.toString() }
            .map { task -> relationsResponseMapper.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskRelationsStateResponse(tasks = it) }
    }

    fun searchTaskResultStates(stateRequest: TaskResultStateSearchRequest): TaskResultStateSearchResponse{
        logger.debug { "Search for ${stateRequest.taskIds.size} task result states" }

        val uuidsToSearch = stateRequest.taskIds.map { toUUID(it) }.toSet()
        val tasksByUuid  = relationsTaskRepository.findByUuidIn(uuidsToSearch).associateBy { it.uuid }

        return TaskResultStateSearchResponse(uuidsToSearch
            .map { tasksByUuid[it]?.processingState?.resultState }
            .map { it?.let { relationsResponseMapper.toResultState(it) }
            })
    }

    @Transactional
    fun reserveTasksForStep(reservationRequest: TaskStepReservationRequest): TaskRelationsStepReservationResponse {
        logger.debug { "Reservation of next relations golden record tasks: executing reserveTasksForStep() with parameters $reservationRequest" }
        val now = Instant.now()

        val foundTasks = relationsTaskRepository.findByStepAndStepState(reservationRequest.step, RelationsGoldenRecordTaskDb.StepState.Queued, Pageable.ofSize(reservationRequest.amount))
            .content.toSet()
            .also { relationsTaskRepository.fetchRelationsData(it) }
        val reservedTasks = foundTasks.map { relationsGoldenRecordTaskStateMachine.doReserve(it) }
        val pendingTimeout = reservedTasks.minOfOrNull { calculateTaskPendingTimeout(it) } ?: now

        logger.debug { "Reserved ${reservedTasks.size} relation golden record tasks for step ${reservationRequest.step}: ${reservedTasks.toLogIdentifiers()}" }

        return reservedTasks
            .map { task ->
                TaskRelationsStepReservationEntryDto(
                    task.uuid.toString(),
                    task.gateRecord.publicId.toString(),
                    relationsResponseMapper.toBusinessPartneRelationsResult(task.businessPartnerRelations)
                )
            }
            .let { reservations -> TaskRelationsStepReservationResponse(reservations, pendingTimeout) }
    }

    @Transactional
    fun resolveStepResults(resultRequest: TaskRelationsStepResultRequest) {
        logger.debug { "Step results for reserved relations golden record tasks: executing resolveStepResults() with parameters $resultRequest" }
        val uuids = resultRequest.results.map { toUUID(it.taskId) }
        val foundTasks = relationsTaskRepository.findByUuidIn(uuids.toSet()).also { relationsTaskRepository.fetchRelationsData(it) }
        val foundTasksByUuid = foundTasks.associateBy { it.uuid.toString() }

        val resolvedTasks = resultRequest.results
            .map { resultEntry -> Pair(foundTasksByUuid[resultEntry.taskId] ?: throw BpdmTaskNotFoundException(resultEntry.taskId), resultEntry) }
            .filterNot { (task, _) -> task.processingState.resultState == RelationsGoldenRecordTaskDb.ResultState.Aborted }
            .mapNotNull { (task, resultEntry) ->
                val step = resultRequest.step
                val errors = resultEntry.errors
                val resultBusinessPartnerRelaitons = resultEntry.businessPartnerRelations

                when{
                    errors.isNotEmpty() -> relationsGoldenRecordTaskStateMachine.doResolveTaskToError(task, step, errors)
                    else ->  relationsGoldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, step, resultBusinessPartnerRelaitons)
                }
            }

        logResolvedTasks(resolvedTasks, resultRequest.step)
    }

    private fun logResolvedTasks(resolvedTasks: List<RelationsGoldenRecordTaskDb>, step: TaskStep) {
        val tasksByResultState = resolvedTasks.groupBy { it.processingState.resultState }

        tasksByResultState[RelationsGoldenRecordTaskDb.ResultState.Pending]
            ?.groupBy { it.processingState.step }
            ?.forEach { (nextStep, tasks) ->
                logger.info { "Advanced ${tasks.size} relation golden record tasks from step $step to step $nextStep: ${tasks.toLogIdentifiers()}" }
            }
        tasksByResultState[RelationsGoldenRecordTaskDb.ResultState.Success]
            ?.let { tasks -> logger.info { "Completed ${tasks.size} relation golden record tasks after step $step: ${tasks.toLogIdentifiers()}" } }
        tasksByResultState[RelationsGoldenRecordTaskDb.ResultState.Error]
            ?.let { tasks -> logger.info { "Failed ${tasks.size} relation golden record tasks in step $step: ${tasks.toLogIdentifiers()}" } }
    }

    private fun getOrCreateGateRecords(requests: List<TaskCreateRelationsRequestEntry>): List<SharingMemberRecordDb> {
        val privateIds = requests.map { request -> request.recordId?.let { toUUID(it) } }
        val notNullPrivateIds = privateIds.filterNotNull()

        val foundRecords = sharingMemberRecordRepository.findByPrivateIdIn(notNullPrivateIds.toSet())
        val foundRecordsByPrivateId = foundRecords.associateBy { it.privateId }
        val requestedNotFoundRecords = notNullPrivateIds.minus(foundRecordsByPrivateId.keys)

        if (requestedNotFoundRecords.isNotEmpty())
            throw BpdmRecordNotFoundException(requestedNotFoundRecords)

        return privateIds.map { privateId ->
            val gateRecord = privateId?.let { foundRecordsByPrivateId[it] } ?: SharingMemberRecordDb(publicId = UUID.randomUUID(), privateId = UUID.randomUUID(), isGoldenRecordCounted = true)
            sharingMemberRecordRepository.save(gateRecord)
        }
    }

    private fun toUUID(uuidString: String) =
        try {
            UUID.fromString(uuidString)
        } catch (e: IllegalArgumentException) {
            throw BpdmTaskNotFoundException(uuidString)
        }

    private fun abortOutdatedTasks(records: Set<SharingMemberRecordDb>){
        val abortedTasks = relationsTaskRepository.findTasksByGateRecordInAndProcessingStateResultState(records, RelationsGoldenRecordTaskDb.ResultState.Pending)
            .map { task -> relationsGoldenRecordTaskStateMachine.doAbortTask(task) }

        if (abortedTasks.isNotEmpty())
            logger.info { "Aborted ${abortedTasks.size} outdated relation golden record tasks: ${abortedTasks.toLogIdentifiers()}" }
    }

    private fun calculateTaskRetentionTimeout(task: RelationsGoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskRetentionTimeout)

    private fun calculateTaskPendingTimeout(task: RelationsGoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskPendingTimeout)

}

private fun Collection<RelationsGoldenRecordTaskDb>.toLogIdentifiers() =
    map { it.uuid.toString() }.joinIdentifiersForLog()