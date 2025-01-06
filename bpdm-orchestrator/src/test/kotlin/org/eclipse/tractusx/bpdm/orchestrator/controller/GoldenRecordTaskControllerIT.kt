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

package org.eclipse.tractusx.bpdm.orchestrator.controller

import org.assertj.core.api.Assertions.*
import org.assertj.core.api.ThrowableAssert
import org.assertj.core.data.TemporalUnitOffset
import org.eclipse.tractusx.bpdm.orchestrator.config.StateMachineConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.OriginRegistrarDb
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.OriginRegistrarRepository
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.BusinessPartnerTestDataFactory
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.Collectors

val WITHIN_ALLOWED_TIME_OFFSET: TemporalUnitOffset = within(1, ChronoUnit.SECONDS)

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "bpdm.security.enabled=false",
        "bpdm.api.upsert-limit=3",
        "bpdm.task.timeoutCheckCron=* * * * * ?",       // check every sec
        "bpdm.task.taskPendingTimeout=3s",
        "bpdm.task.taskRetentionTimeout=5s"
    ]
)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class GoldenRecordTaskControllerIT @Autowired constructor(
    private val orchestratorClient: OrchestrationApiClient,
    private val taskConfigProperties: TaskConfigProperties,
    private val dbTestHelpers: DbTestHelpers,
    private val stateMachineConfigProperties: StateMachineConfigProperties,
    private val originRegistrarRepository: OriginRegistrarRepository,
    private val goldenRecordTaskRepository: GoldenRecordTaskRepository
) {

    private val testDataFactory = BusinessPartnerTestDataFactory()
    private val defaultBusinessPartner1 = testDataFactory.createFullBusinessPartner("BP1")
    private val defaultBusinessPartner2 = testDataFactory.createFullBusinessPartner("BP2")
    private val originId = "test-origin"

    @BeforeEach
    fun cleanUp() {
        dbTestHelpers.truncateDbTables()
        originRegistrarRepository.deleteAll()
        originRegistrarRepository.save(OriginRegistrarDb(originId = originId, name = "test", priority = PriorityEnum.High, threshold = 1))
    }

    /**
     * GIVEN no tasks
     * WHEN creating some tasks in task mode
     *  THEN expect create response contains correct processingState with step
     * WHEN checking state
     *  THEN expect same state as in create response
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `request cleaning task`(taskMode: TaskMode) {
        // create tasks and check response
        val createdTasks = createTasksWithoutRecordId(mode = taskMode).createdTasks
        val expectedStep = stateMachineConfigProperties.modeSteps[taskMode]!!.first()

        assertThat(createdTasks.size).isEqualTo(2)

        assertThat(createdTasks[0].taskId).isNotEqualTo(createdTasks[1].taskId)

        createdTasks.forEach { stateDto ->
            val processingState = stateDto.processingState
            assertProcessingStateDto(processingState, ResultState.Pending, expectedStep, StepState.Queued)
            assertThat(processingState.errors).isEqualTo(emptyList<TaskErrorDto>())
        }

        // check if response is consistent with searchTaskStates response
        val statesResponse = searchTaskStates(createdTasks.map { it.toTaskSearchIdentity() })
        assertThat(statesResponse.tasks).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(createdTasks)
    }


    /**
     * GIVEN some tasks were created
     * WHEN reserving some tasks in first step
     *  THEN expect reservation returns the correct number of entries containing the correct business partner information
     * WHEN trying to reserve more tasks
     *  THEN expect no additional results
     * WHEN checking state
     *  THEN expect correct stepState (Reserved)
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `request reservation`(taskMode: TaskMode) {
        // create tasks
        val createdTasks = createTasksWithoutRecordId(taskMode).createdTasks
        assertThat(createdTasks.size).isEqualTo(2)

        val expectedStep = stateMachineConfigProperties.modeSteps[taskMode]!!.first()

        // reserve tasks
        val reservationResponse1 = reserveTasks(expectedStep)
        val reservedTasks = reservationResponse1.reservedTasks

        // expect the correct number of entries with the correct timeout
        assertThat(reservedTasks.size).isEqualTo(2)
        assertThat(reservedTasks.map { it.taskId }).isEqualTo(createdTasks.map { it.taskId })

        // ...and with the correct business partner information
        assertThat(reservedTasks[0].businessPartner).isEqualTo(defaultBusinessPartner1)
        assertThat(reservedTasks[1].businessPartner).isEqualTo(defaultBusinessPartner2)

        // trying to reserve more tasks returns no additional entries
        val reservationResponse2 = reserveTasks(expectedStep)
        assertThat(reservationResponse2.reservedTasks.size).isEqualTo(0)

        // check searchTaskStates response
        val statesResponse = createdTasks.searchTaskStates(reservedTasks.map { it.taskId })
        assertThat(statesResponse.tasks.size).isEqualTo(2)
        statesResponse.tasks.forEach { stateDto ->
            val processingState = stateDto.processingState
            // stepState should have changed to Reserved
            assertProcessingStateDto(processingState, ResultState.Pending, expectedStep, StepState.Reserved)
            assertThat(processingState.errors).isEqualTo(emptyList<TaskErrorDto>())
            assertThat(processingState.modifiedAt).isAfter(processingState.createdAt)
            assertThat(processingState.modifiedAt).isCloseTo(Instant.now(), WITHIN_ALLOWED_TIME_OFFSET)
        }
    }

    /**
     * GIVEN some tasks were created
     * WHEN reserving some tasks in wrong step
     *  THEN expect reservation returns no results
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `request reservation for wrong step`(taskMode: TaskMode) {
        // create tasks
        createTasksWithoutRecordId(taskMode)

        // try reservation for wrong step
        val wrongStep = TaskStep.entries.find { it != stateMachineConfigProperties.modeSteps[taskMode]!!.first() }!!
        val reservedTasks = reserveTasks(wrongStep).reservedTasks
        assertThat(reservedTasks.size).isEqualTo(0)
    }

    /**
     * GIVEN some tasks were created
     * WHEN reserving one task in correct step
     *  THEN expect first task and state to switch to stepState==Reserved
     * WHEN resolving this task
     *  THEN expect state to switch to next step and stepState==Queued
     * WHEN repeating reserving and resolving through all steps
     *  THEN expect state to switch to resultState==Success and stepState==Success and correct business partner data
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `post cleaning results for all steps`(taskMode: TaskMode) {
        val allSteps = stateMachineConfigProperties.modeSteps[taskMode]!!

        // create tasks
        val createdTasks = createTasksWithoutRecordId(mode = taskMode, businessPartners = listOf(defaultBusinessPartner1)).createdTasks
        val taskId = createdTasks.single().taskId

        var expectedBusinessPartner = createdTasks.single().businessPartnerResult
        allSteps.forEach { step ->
            assertProcessingStateDto(
                createdTasks.searchTaskStates(listOf(taskId)).tasks.single().processingState,
                ResultState.Pending, step, StepState.Queued
            )

            val reservedTask = reserveTasks(step, 1).reservedTasks.single()
            assertThat(reservedTask.businessPartner).isEqualTo(expectedBusinessPartner)

            // now in stepState==Reserved
            assertProcessingStateDto(
                createdTasks.searchTaskStates(listOf( reservedTask.taskId)).tasks.single().processingState,
                ResultState.Pending, step, StepState.Reserved
            )

            // resolve task
            val businessPartnerResolved = testDataFactory.createFullBusinessPartner("Resolved BP")
            val resultEntry = TaskStepResultEntryDto(
                taskId = reservedTask.taskId,
                businessPartner = businessPartnerResolved
            )
            resolveTasks(step, listOf(resultEntry))

            expectedBusinessPartner = businessPartnerResolved
        }

        // final step -> now in stepState==Success
        val finalStateDto = createdTasks.searchTaskStates(listOf(taskId)).tasks.single()
        assertProcessingStateDto(
            finalStateDto.processingState,
            ResultState.Success, stateMachineConfigProperties.modeSteps[taskMode]!!.last(), StepState.Success
        )
        // check returned BP
        assertThat(finalStateDto.businessPartnerResult).isEqualTo(expectedBusinessPartner)
    }

    /**
     * GIVEN some tasks were created and reserved
     * WHEN resolving this task an error
     *  THEN expect state to switch to resultState==Error and stepState==Error
     * WHEN reserving this task
     *  THEN expect state to switch to stepState==Reserved
     * WHEN resolving this task
     *  THEN expect state to switch to resultState==Success and stepState==Success and correct business partner data
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `post cleaning result with error`(taskMode: TaskMode) {
        // create tasks
        val createdTasks = createTasksWithoutRecordId(taskMode).createdTasks
        val firstStep = stateMachineConfigProperties.modeSteps[taskMode]!!.first()

        // reserve task
        val taskId = reserveTasks(firstStep, 1).reservedTasks.single().taskId

        // resolve task with error
        val errorDto = TaskErrorDto(TaskErrorType.Unspecified, "Unfortunate event")
        val resultEntry = TaskStepResultEntryDto(
            taskId = taskId,
            businessPartner = defaultBusinessPartner1,
            errors = listOf(errorDto)
        )
        resolveTasks(firstStep, listOf(resultEntry))

        // now in error state
        val stateDto = createdTasks.searchTaskStates(listOf(taskId)).tasks.single()
        assertProcessingStateDto(
            stateDto.processingState,
            ResultState.Error, firstStep, StepState.Error
        )

        // expect error in response
        assertThat(stateDto.processingState.errors.single()).isEqualTo(errorDto)
    }

    /**
     * WHEN requesting cleaning of too many business partners (over the upsert limit)
     * THEN throw exception
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `expect exception on requesting too many cleaning tasks`(taskMode: TaskMode) {
        // Create entries above the upsert limit of 3
        val businessPartners = listOf(
            testDataFactory.createFullBusinessPartner("BP1"),
            testDataFactory.createFullBusinessPartner("BP2"),
            testDataFactory.createFullBusinessPartner("BP3"),
            testDataFactory.createFullBusinessPartner("BP4")
        )

        assertBadRequestException {
            createTasksWithoutRecordId(mode = taskMode, businessPartners = businessPartners)
        }
    }

    /**
     * WHEN reserving too many cleaning tasks (over the upsert limit)
     * THEN throw exception
     */
    @ParameterizedTest
    @EnumSource(TaskStep::class)
    fun `expect exception on requesting too many reservations`(step: TaskStep) {
        // Create entries above the upsert limit of 3
        assertBadRequestException {
            reserveTasks(step, 200)
        }
    }

    /**
     * WHEN posting too many cleaning results (over the upsert limit)
     * THEN throw exception
     */
    @ParameterizedTest
    @EnumSource(TaskStep::class)
    fun `expect exception on posting too many task results`(step: TaskStep) {
        val validResultEntry = TaskStepResultEntryDto(
            taskId = "0",
            businessPartner = defaultBusinessPartner1,
            errors = listOf(TaskErrorDto(type = TaskErrorType.Unspecified, description = "Description"))
        )

        // Create entries above the upsert limit of 3
        val resultEntries = listOf(
            validResultEntry.copy(taskId = "0"),
            validResultEntry.copy(taskId = "1"),
            validResultEntry.copy(taskId = "2"),
            validResultEntry.copy(taskId = "3"),
        )

        assertBadRequestException {
            resolveTasks(step, resultEntries)
        }
    }

    /**
     * GIVEN some resolved tasks
     * WHEN trying to resolve a task with different task id
     *  THEN expect a BAD_REQUEST
     * WHEN trying to resolve a task with empty content
     *  THEN expect a BAD_REQUEST
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `expect exceptions on posting inconsistent task results`(taskMode: TaskMode) {
        // create tasks
        createTasksWithoutRecordId(taskMode)
        val firstStep = stateMachineConfigProperties.modeSteps[taskMode]!!.first()

        // reserve tasks
        val tasksIds = reserveTasks(firstStep).reservedTasks.map { it.taskId }
        assertThat(tasksIds.size).isEqualTo(2)

        // post wrong task ids
        assertBadRequestException {
            resolveTasks(
                firstStep,
                listOf(
                    TaskStepResultEntryDto(
                        taskId = "WRONG-ID",
                        businessPartner = defaultBusinessPartner1
                    )
                )
            )
        }

        // post correct task id but wrong step
        val wrongStep = TaskStep.entries.find{ it != firstStep}!!
        assertBadRequestException {
            resolveTasks(
                wrongStep,
                listOf(
                    TaskStepResultEntryDto(
                        taskId = tasksIds[0],
                        businessPartner = defaultBusinessPartner1
                    )
                )
            )
        }

        // post correct task id with business partner content
        resolveTasks(
            firstStep,
            listOf(
                TaskStepResultEntryDto(
                    taskId = tasksIds[0],
                    businessPartner = defaultBusinessPartner1
                )
            )
        )

        // post correct task id with error content
        resolveTasks(
            firstStep,
            listOf(
                TaskStepResultEntryDto(
                    tasksIds[1],
                    businessPartner = defaultBusinessPartner1,
                    errors = listOf(
                        TaskErrorDto(type = TaskErrorType.Unspecified, description = "ERROR")
                    )
                )
            )
        )
    }

    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `wait for task pending and retention timeout`(taskMode: TaskMode) {
        // create tasks
        val createdTasks = createTasksWithoutRecordId(taskMode).createdTasks
        val taskIds = createdTasks.map { it.toTaskSearchIdentity() }

        // check for state Pending
        checkStateForAllTasks(taskIds) {
            assertThat(it.resultState).isEqualTo(ResultState.Pending)
        }

        // wait for 1/2 pending time -> should still be pending
        Thread.sleep(taskConfigProperties.taskPendingTimeout.dividedBy(2).toMillis())
        checkStateForAllTasks(taskIds) {
            assertThat(it.resultState).isEqualTo(ResultState.Pending)
        }

        // wait for another 1/2 pending time plus 1sec -> should be in state Error / Timeout
        Thread.sleep(taskConfigProperties.taskPendingTimeout.dividedBy(2).plusSeconds(1).toMillis())
        checkStateForAllTasks(taskIds) {
            assertThat(it.resultState).isEqualTo(ResultState.Error)
            assertThat(it.errors.first().type).isEqualTo(TaskErrorType.Timeout)
        }

        // wait for 1/2 retention time -> should still be in state Error / Timeout
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).toMillis())
        checkStateForAllTasks(taskIds) {
            assertThat(it.resultState).isEqualTo(ResultState.Error)
        }

        // wait for 1/2 retention time plus 1sec -> should be removed now
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).plusSeconds(1).toMillis())
        val foundTasks = searchTaskStates(taskIds).tasks
        assertThat(foundTasks.size).isZero()
    }

    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `wait for task retention timeout after success`(taskMode: TaskMode) {
        // create single task in UpdateFromPool mode (only one step)
        val createdTasks = createTasksWithoutRecordId(taskMode, listOf(defaultBusinessPartner1)).createdTasks
        val createdTask = createdTasks.single()
        val taskId = createdTask.taskId

        val allSteps = stateMachineConfigProperties.modeSteps[taskMode]!!
        allSteps.forEach { step ->
            reserveTasks(step).reservedTasks.single()
            // resolve with success
            val cleaningResult = TaskStepResultEntryDto(
                taskId = taskId,
                businessPartner = createdTask.businessPartnerResult
            )
            resolveTasks(step, listOf(cleaningResult))
        }

        // should be in state Success now
        createdTasks.searchTaskStates(listOf(taskId)).tasks.forEach {
            assertThat(it.processingState.resultState).isEqualTo(ResultState.Success)
        }

        // wait for 1/2 retention time -> should still be in state Success
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).toMillis())
        createdTasks.searchTaskStates(listOf(taskId)).tasks.forEach {
            assertThat(it.processingState.resultState).isEqualTo(ResultState.Success)
        }

        // wait for 1/2 retention time -> should still be removed
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).plusSeconds(1).toMillis())
        val foundTasks = createdTasks.searchTaskStates(listOf(taskId)).tasks
        assertThat(foundTasks.size).isZero()
    }

    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `wait for task retention timeout after error`(taskMode: TaskMode) {
        // create single task
        val createdTasks = createTasksWithoutRecordId(taskMode, listOf(defaultBusinessPartner1)).createdTasks
        val firstStep = stateMachineConfigProperties.modeSteps[taskMode]!!.first()

        // reserve task
        val reservedTask = reserveTasks(firstStep).reservedTasks.single()
        val taskId = reservedTask.taskId
        val searchIdentity = reservedTask.toTaskSearchIdentity(createdTasks)

        // resolve with error
        val cleaningResult = TaskStepResultEntryDto(
            taskId = taskId,
            businessPartner = reservedTask.businessPartner,
            errors = listOf(TaskErrorDto(TaskErrorType.Unspecified, "Unfortunate event"))
        )
        resolveTasks(firstStep, listOf(cleaningResult))

        // should be in state Success now
        checkStateForAllTasks(listOf(searchIdentity)) {
            assertThat(it.resultState).isEqualTo(ResultState.Error)
        }

        // wait for 1/2 retention time -> should still be in state Success
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).toMillis())
        checkStateForAllTasks(listOf(searchIdentity)) {
            assertThat(it.resultState).isEqualTo(ResultState.Error)
        }

        // wait for 1/2 retention time -> should still be removed
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).plusSeconds(1).toMillis())
        val foundTasks = searchTaskStates(listOf(searchIdentity)).tasks
        assertThat(foundTasks.size).isZero()
    }

    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `create task for existing gate record`(taskMode: TaskMode){
        //Create records by creating tasks first
        val existingRecordIds = createTasksWithoutRecordId(taskMode).createdTasks.map { it.recordId }

        val requestsWithRecords = listOf(defaultBusinessPartner1, defaultBusinessPartner2)
            .zip(existingRecordIds)
            .map { (bp, recordId) -> TaskCreateRequestEntry(recordId, bp) }

        requestsWithRecords.forEach { assertThat(it.recordId).isNotNull() }

        val tasksWithRecords = orchestratorClient.goldenRecordTasks.createTasks(TaskCreateRequest(taskMode, requestsWithRecords, originId)).createdTasks

        tasksWithRecords.zip(existingRecordIds).forEach { (actualTask, expectedRecordId) -> assertThat(actualTask.recordId).isEqualTo(expectedRecordId) }

        var goldenRecordTaskDb = goldenRecordTaskRepository.findByUuidIn(tasksWithRecords.filter { it.processingState.resultState==ResultState.Pending }.map { UUID.fromString(it.taskId) }.toSet())

        Assertions.assertSame(1, goldenRecordTaskDb.filter { it.priority == PriorityEnum.High }.count())
    }

    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `expect exception on creating task for non-existing gate record`(taskMode: TaskMode){
        val unknownRecordId = UUID.randomUUID()

        val requestWithUnknownRecord = TaskCreateRequestEntry(unknownRecordId.toString(), defaultBusinessPartner1)

        assertBadRequestException{
            createTasks(mode = taskMode, entries = listOf(requestWithUnknownRecord))
        }
    }

    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `abort task when outdated`(taskMode: TaskMode){
        val createdTasks  = createTasks(mode = taskMode, entries = listOf(defaultBusinessPartner1, defaultBusinessPartner2).map { TaskCreateRequestEntry(null, it) }).createdTasks
        val createdTaskIds = createdTasks.map { it.toTaskSearchIdentity() }
        val createdRecordIds = createdTasks.map { it.recordId }

        //Create newer tasks for the given records
        createTasks(mode = taskMode, entries = createdRecordIds
            .zip(listOf(defaultBusinessPartner1, defaultBusinessPartner2))
            .map { (recordId, bp) -> TaskCreateRequestEntry(recordId, bp) }
        ).createdTasks

        val olderTasks = searchTaskStates(createdTaskIds).tasks

        olderTasks.forEach { assertThat(it.processingState.resultState).isEqualTo(ResultState.Error) }
    }

    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `aborted task throws no error when trying to resolve`(taskMode: TaskMode){
        val createdTasks  = createTasks(mode = taskMode, entries = listOf(defaultBusinessPartner1, defaultBusinessPartner2).map { TaskCreateRequestEntry(null, it) }).createdTasks
        val createdRecordIds = createdTasks.map { it.recordId }
        val firstStep = stateMachineConfigProperties.modeSteps[taskMode]!!.first()

        val reservedTasks = reserveTasks(firstStep, amount = 2).reservedTasks

        //Create newer tasks for the given records
        createTasks(mode = taskMode, entries = createdRecordIds
            .zip(listOf(defaultBusinessPartner1, defaultBusinessPartner2))
            .map { (recordId, bp) -> TaskCreateRequestEntry(recordId, bp) }
        ).createdTasks

        assertDoesNotThrow {
            resolveTasks(firstStep, reservedTasks.map { TaskStepResultEntryDto(it.taskId, it.businessPartner) })
        }
    }

    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `ignore resolution for already resolved steps`(taskMode: TaskMode){
        val allSteps = stateMachineConfigProperties.modeSteps[taskMode]!!

        // create tasks
        val createdTasks = createTasksWithoutRecordId(mode = taskMode, businessPartners = listOf(defaultBusinessPartner1, defaultBusinessPartner2)).createdTasks

        var expectedBusinessPartners = createdTasks.map { it.businessPartnerResult }
        allSteps.forEach { step ->
            val reservedTasks = reserveTasks(step, 2).reservedTasks

            val resolvedBusinessPartners = reservedTasks.map {
                TaskStepResultEntryDto(
                    taskId = it.taskId,
                    businessPartner =  testDataFactory.createFullBusinessPartner("${it.taskId} $step Resolved")
                )
            }
            resolveTasks(step, resolvedBusinessPartners)
            expectedBusinessPartners = resolvedBusinessPartners.map { it.businessPartner }

            val stepsSoFar = stateMachineConfigProperties.modeSteps[taskMode]!!.takeWhile { it != step } + step
            stepsSoFar.forEach { previousStep ->
                // accept and ignore resolving already resolved tasks
                reservedTasks.map {
                    TaskStepResultEntryDto(
                        taskId = it.taskId,
                        businessPartner =  testDataFactory.createFullBusinessPartner("${it.taskId} $previousStep Resolved Again")
                    )
                }
                resolveTasks(step, resolvedBusinessPartners)
            }
        }

        // final step -> now in stepState==Success
        val finalTaskStates = createdTasks.searchTaskStates(createdTasks.map { it.taskId }).tasks
        finalTaskStates.forEach { task ->
            assertProcessingStateDto(
                task.processingState,
                ResultState.Success, stateMachineConfigProperties.modeSteps[taskMode]!!.last(), StepState.Success
            )
        }
        // check returned BP
        assertThat(finalTaskStates.map { it.businessPartnerResult })
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedBusinessPartners)


    }


    private fun createTasks(mode: TaskMode,
                            entries: List<TaskCreateRequestEntry>? = null
    ): TaskCreateResponse{
        val resolvedEntries = entries ?: listOf(defaultBusinessPartner1, defaultBusinessPartner2).map { bp -> TaskCreateRequestEntry(null, bp) }
        return orchestratorClient.goldenRecordTasks.createTasks(TaskCreateRequest(mode = mode, requests = resolvedEntries, originId = originId))
    }

    private fun createTasksWithoutRecordId(mode: TaskMode, businessPartners: List<BusinessPartner>? = null): TaskCreateResponse =
        createTasks(mode, (businessPartners ?: listOf(defaultBusinessPartner1, defaultBusinessPartner2)).map{ bp -> TaskCreateRequestEntry(null, bp) })

    private fun reserveTasks(step: TaskStep, amount: Int = 3) =
        orchestratorClient.goldenRecordTasks.reserveTasksForStep(
            TaskStepReservationRequest(
                step = step,
                amount = amount
            )
        )

    private fun resolveTasks(step: TaskStep, results: List<TaskStepResultEntryDto>) =
        orchestratorClient.goldenRecordTasks.resolveStepResults(
            TaskStepResultRequest(step, results)
        )

    private fun searchTaskStates(taskIds: List<TaskStateRequest.Entry>) =
        orchestratorClient.goldenRecordTasks.searchTaskStates(
            TaskStateRequest(taskIds)
        )

    private fun checkStateForAllTasks(taskIds: List<TaskStateRequest.Entry>, checkFunc: (TaskProcessingStateDto) -> Unit) {
        searchTaskStates(taskIds).tasks
            .also { assertThat(it.size).isEqualTo(taskIds.size) }
            .forEach { stateDto -> checkFunc(stateDto.processingState) }
    }

    private fun assertProcessingStateDto(processingStateDto: TaskProcessingStateDto, resultState: ResultState, step: TaskStep, stepState: StepState) {
        assertThat(processingStateDto.resultState).isEqualTo(resultState)
        assertThat(processingStateDto.step).isEqualTo(step)
        assertThat(processingStateDto.stepState).isEqualTo(stepState)
    }

    private fun assertBadRequestException(shouldRaiseThrowable: ThrowableAssert.ThrowingCallable) {
        assertThatThrownBy(shouldRaiseThrowable)
            .isInstanceOfSatisfying(WebClientResponseException::class.java) {
                assertThat(it.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            }
    }

    private fun TaskClientStateDto.toTaskSearchIdentity() = TaskStateRequest.Entry(taskId, recordId)

    private fun TaskStepReservationEntryDto.toTaskSearchIdentity(createdTasks: List<TaskClientStateDto>) =
        TaskStateRequest.Entry(taskId, createdTasks.find { it.taskId == taskId }!!.recordId)

    private fun List<TaskClientStateDto>.searchTaskStates(taskIds: List<String>) =
        this.searchTaskStates(taskIds.toSet())

    private fun List<TaskClientStateDto>.searchTaskStates(taskIds: Set<String>) =
            this.filter { taskIds.contains(it.taskId) }
                .map { it.toTaskSearchIdentity() }
                .let { searchTaskStates(it) }


}
