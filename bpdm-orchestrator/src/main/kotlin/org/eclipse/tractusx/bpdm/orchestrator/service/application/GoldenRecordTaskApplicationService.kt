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

package org.eclipse.tractusx.bpdm.orchestrator.service.application

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.DbTimestamp
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.SharingMemberRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmInvalidBusinessPartnerException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.fetchBusinessPartnerData
import org.eclipse.tractusx.bpdm.orchestrator.service.operation.GoldenRecordTaskStateMachine
import org.eclipse.tractusx.bpdm.orchestrator.service.operation.PaginationInfo
import org.eclipse.tractusx.bpdm.orchestrator.service.parser.GoldenRecordTaskResponseParser
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class GoldenRecordTaskApplicationService(
    private val goldenRecordTaskStateMachine: GoldenRecordTaskStateMachine,
    private val taskConfigProperties: TaskConfigProperties,
    private val responseParser: GoldenRecordTaskResponseParser,
    private val taskRepository: GoldenRecordTaskRepository,
    private val sharingMemberRecordService: SharingMemberRecordApplicationService
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createTasks(createRequest: TaskCreateRequest): TaskCreateResponse {
        logger.debug { "Creation of new golden record tasks: executing createTasks() with parameters $createRequest" }

        createRequest.requests.forEach { assertAdditionalSitesHaveSite(it.businessPartner) }

        val gateRecords = sharingMemberRecordService.getOrCreateGateRecords(createRequest.requests)
        abortOutdatedTasks(gateRecords.toSet())

        val createdTasks = createRequest.requests.zip(gateRecords)
            .map { (request, record) -> goldenRecordTaskStateMachine.initTask(createRequest.mode, request.businessPartner, record) }

        if (createdTasks.isNotEmpty())
            logger.info { "Created ${createdTasks.size} golden record tasks in mode ${createRequest.mode}: ${createdTasks.toLogIdentifiers()}" }

        return createdTasks
            .map { task -> responseParser.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskCreateResponse(createdTasks = it) }
    }

    /**
     * Rejects business partner data that states further sites of its address without stating a site of its own, which
     * those sites would be additional to.
     */
    private fun assertAdditionalSitesHaveSite(businessPartner: BusinessPartner) {
        if (businessPartner.additionalSites.isNotEmpty() && businessPartner.site == null)
            throw BpdmInvalidBusinessPartnerException(
                "additional sites of its address are stated but no site of its own is, which they would be additional to"
            )
    }

    fun searchTaskResultStates(stateRequest: TaskResultStateSearchRequest): TaskResultStateSearchResponse{
        logger.debug { "Search for ${stateRequest.taskIds.size} task result states" }

        val uuidsToSearch = stateRequest.taskIds.map { toUUID(it) }.toSet()
        val tasksByUuid  = taskRepository.findByUuidIn(uuidsToSearch).associateBy { it.uuid }

        return TaskResultStateSearchResponse(uuidsToSearch
            .map { tasksByUuid[it]?.processingState?.resultState }
            .map { it?.let { responseParser.toResultState(it) }
            })
    }

    fun searchTaskStates(stateRequest: TaskStateRequest): TaskStateResponse {
        logger.debug { "Search for the state of golden record task: executing searchTaskStates() with parameters $stateRequest" }
        val requestsByTaskId = stateRequest.entries.associateBy { it.taskId }

        return stateRequest.entries.map { toUUID(it.taskId) }
            .let { uuids -> taskRepository.findByUuidIn(uuids.toSet()) }
            .also { tasks -> taskRepository.fetchBusinessPartnerData(tasks) }
            .filter { task -> requestsByTaskId[task.uuid.toString()]?.recordId == task.gateRecord.privateId.toString() }
            .map { task -> responseParser.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskStateResponse(tasks = it) }
    }

    @Transactional
    fun reserveTasksForStep(reservationRequest: TaskStepReservationRequest): TaskStepReservationResponse {
        logger.debug { "Reservation of next golden record tasks: executing reserveTasksForStep() with parameters $reservationRequest" }
        val now = Instant.now()

        val foundTasks = taskRepository.findByStepAndStepState(reservationRequest.step, GoldenRecordTaskDb.StepState.Queued, Pageable.ofSize(reservationRequest.amount))
            .content.toSet()
            .also { taskRepository.fetchBusinessPartnerData(it) }
        val reservedTasks = foundTasks.map { goldenRecordTaskStateMachine.doReserve(it) }
        val pendingTimeout = reservedTasks.minOfOrNull { calculateTaskPendingTimeout(it) } ?: now

        logger.debug { "Reserved ${reservedTasks.size} golden record tasks for step ${reservationRequest.step}: ${reservedTasks.toLogIdentifiers()}" }

        return reservedTasks
            .map { task ->
                TaskStepReservationEntryDto(
                    task.uuid.toString(),
                    task.gateRecord.publicId.toString(),
                    responseParser.toBusinessPartnerResult(task.businessPartner)
                )
            }
            .let { reservations -> TaskStepReservationResponse(reservations, pendingTimeout) }
    }

    @Transactional
    fun resolveStepResults(resultRequest: TaskStepResultRequest) {
        logger.debug { "Step results for reserved golden record tasks: executing resolveStepResults() with parameters $resultRequest" }
        resultRequest.results.filter { it.errors.isEmpty() }.forEach { assertAdditionalSitesHaveSite(it.businessPartner) }

        val uuids = resultRequest.results.map { toUUID(it.taskId) }
        val foundTasks = taskRepository.findByUuidIn(uuids.toSet()).also { taskRepository.fetchBusinessPartnerData(it) }
        val foundTasksByUuid = foundTasks.associateBy { it.uuid.toString() }

        val resolvedTasks = resultRequest.results
            .map { resultEntry -> Pair(foundTasksByUuid[resultEntry.taskId] ?: throw BpdmTaskNotFoundException(resultEntry.taskId), resultEntry) }
            .filterNot { (task, _) -> task.processingState.resultState == GoldenRecordTaskDb.ResultState.Aborted }
            .mapNotNull { (task, resultEntry) ->
                val step = resultRequest.step
                val errors = resultEntry.errors
                val resultBusinessPartner = resultEntry.businessPartner

                when{
                    errors.isNotEmpty() -> goldenRecordTaskStateMachine.doResolveTaskToError(task, step, errors)
                    else ->  goldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, step, resultBusinessPartner)
                }
            }

        logResolvedTasks(resolvedTasks, resultRequest.step)
    }

    private fun logResolvedTasks(resolvedTasks: List<GoldenRecordTaskDb>, step: TaskStep) {
        val tasksByResultState = resolvedTasks.groupBy { it.processingState.resultState }

        tasksByResultState[GoldenRecordTaskDb.ResultState.Pending]
            ?.groupBy { it.processingState.step }
            ?.forEach { (nextStep, tasks) ->
                logger.info { "Advanced ${tasks.size} golden record tasks from step $step to step $nextStep: ${tasks.toLogIdentifiers()}" }
            }
        tasksByResultState[GoldenRecordTaskDb.ResultState.Success]
            ?.let { tasks -> logger.info { "Completed ${tasks.size} golden record tasks after step $step: ${tasks.toLogIdentifiers()}" } }
        tasksByResultState[GoldenRecordTaskDb.ResultState.Error]
            ?.let { tasks -> logger.info { "Failed ${tasks.size} golden record tasks in step $step: ${tasks.toLogIdentifiers()}" } }
    }

    @Transactional
    fun processPendingTimeouts(pageSize: Int): PaginationInfo {
        val timedOutTasks = mutableListOf<GoldenRecordTaskDb>()

        return batchProcessTasks(pageSize,
            fetchPage = { pageable -> taskRepository.findByProcessingStatePendingTimeoutBefore(DbTimestamp.now(), pageable) },
            processTask = { task ->
                goldenRecordTaskStateMachine.doResolveTaskToTimeout(task)
                timedOutTasks.add(task)
            }
        ).also {
            if (timedOutTasks.isNotEmpty())
                logger.info { "Timed out ${timedOutTasks.size} golden record tasks: ${timedOutTasks.toLogIdentifiers()}" }
        }
    }

    @Transactional
    fun processRetentionTimeouts(pageSize: Int): PaginationInfo {
        val deletedTasks = mutableListOf<GoldenRecordTaskDb>()

        return batchProcessTasks(pageSize,
            fetchPage = { pageable -> taskRepository.findByProcessingStateRetentionTimeoutBefore(DbTimestamp.now(), pageable) },
            processTask = { task ->
                taskRepository.delete(task)
                deletedTasks.add(task)
            }
        ).also {
            if (deletedTasks.isNotEmpty())
                logger.info { "Deleted ${deletedTasks.size} golden record tasks after their retention timeout: ${deletedTasks.toLogIdentifiers()}" }
        }
    }

    private fun batchProcessTasks(
        pageSize: Int,
        fetchPage: (Pageable) -> Page<GoldenRecordTaskDb>,
        processTask: (GoldenRecordTaskDb) -> Unit
    ): PaginationInfo {
        val pageable: Pageable = PageRequest.of(0, pageSize)
        val page = fetchPage(pageable)
        var hasProcessedTasks = false
        var processedTaskCount = 0

        page.forEach { task ->
            try {
                processTask(task)
                hasProcessedTasks = true
                processedTaskCount++ // Increment on successful processing
            } catch (err: RuntimeException) {
                logger.error(err) { "Error processing timeout for task ${task.uuid}" }
            }
        }

        return PaginationInfo(hasProcessedTasks, page.hasNext(), processedTaskCount)
    }

    private fun calculateTaskPendingTimeout(task: GoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskPendingTimeout)

    private fun calculateTaskRetentionTimeout(task: GoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskRetentionTimeout)

    private fun toUUID(uuidString: String) =
        try {
            UUID.fromString(uuidString)
        } catch (e: IllegalArgumentException) {
            throw BpdmTaskNotFoundException(uuidString)
        }

    private fun abortOutdatedTasks(records: Set<SharingMemberRecordDb>){
        val abortedTasks = taskRepository.findTasksByGateRecordInAndProcessingStateResultState(records, GoldenRecordTaskDb.ResultState.Pending)
            .map { task -> goldenRecordTaskStateMachine.doAbortTask(task) }

        if (abortedTasks.isNotEmpty())
            logger.info { "Aborted ${abortedTasks.size} outdated golden record tasks: ${abortedTasks.toLogIdentifiers()}" }
    }
}

private fun Collection<GoldenRecordTaskDb>.toLogIdentifiers() =
    map { it.uuid.toString() }.joinIdentifiersForLog()
