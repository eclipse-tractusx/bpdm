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

package org.eclipse.tractusx.bpdm.orchestrator.controller

import org.assertj.core.api.Assertions.*
import org.assertj.core.api.ThrowableAssert
import org.assertj.core.data.TemporalUnitOffset
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.service.GoldenRecordTaskStorage
import org.eclipse.tractusx.bpdm.orchestrator.testdata.BusinessPartnerTestValues
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant
import java.time.temporal.ChronoUnit

val WITHIN_ALLOWED_TIME_OFFSET: TemporalUnitOffset = within(10, ChronoUnit.SECONDS)

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "bpdm.api.upsert-limit=3",
        "bpdm.task.task-timeout=12h"
    ]
)
class GoldenRecordTaskControllerIT @Autowired constructor(
    val orchestratorClient: OrchestrationApiClient,
    val taskConfigProperties: TaskConfigProperties,
    val goldenRecordTaskStorage: GoldenRecordTaskStorage
) {

    @BeforeEach
    fun cleanUp() {
        goldenRecordTaskStorage.clear()
    }

    /**
     * GIVEN no tasks
     * WHEN creating some tasks in UpdateFromSharingMember mode
     *  THEN expect create response contains correct processingState with step==CleanAndSync
     * WHEN checking state
     *  THEN expect same state as in create response
     */
    @Test
    fun `request cleaning task`() {
        // create tasks and check response
        val createdTasks = createTasks().createdTasks

        val expectedTimeout = Instant.now().plus(taskConfigProperties.taskTimeout)

        assertThat(createdTasks.size).isEqualTo(2)

        assertThat(createdTasks[0].taskId).isNotEqualTo(createdTasks[1].taskId)

        createdTasks.forEach { stateDto ->
            assertThat(stateDto.businessPartnerResult).isNull()
            val processingState = stateDto.processingState
            assertProcessingStateDto(processingState, ResultState.Pending, TaskStep.CleanAndSync, StepState.Queued)
            assertThat(processingState.errors).isEqualTo(emptyList<TaskErrorDto>())
            assertThat(processingState.createdAt).isEqualTo(processingState.modifiedAt)
            assertThat(processingState.timeout).isCloseTo(expectedTimeout, WITHIN_ALLOWED_TIME_OFFSET)
        }

        // check if response is consistent with searchTaskStates response
        val statesResponse = searchTaskStates(createdTasks.map { it.taskId })
        assertThat(statesResponse.tasks).isEqualTo(createdTasks)
    }

    /**
     * GIVEN no tasks
     * WHEN creating some tasks in UpdateFromPool mode
     *  THEN expect create response contains correct processingState with step==Clean
     */
    @Test
    fun `request cleaning task in alternative mode`() {
        // create tasks and check response
        val createdTasks = createTasks(TaskMode.UpdateFromPool).createdTasks

        assertThat(createdTasks.size).isEqualTo(2)
        val processingState = createdTasks[0].processingState

        // Mode "UpdateFromPool" should trigger "Clean" step
        assertProcessingStateDto(processingState, ResultState.Pending, TaskStep.Clean, StepState.Queued)
    }

    /**
     * GIVEN some tasks were created in UpdateFromSharingMember mode
     * WHEN reserving some tasks in step CleanAndSync
     *  THEN expect reservation returns the correct number of entries containing the correct business partner information
     * WHEN trying to reserve more tasks
     *  THEN expect no additional results
     * WHEN checking state
     *  THEN expect correct stepState (Reserved)
     */
    @Test
    fun `request reservation`() {
        // create tasks
        val createdTasks = createTasks(TaskMode.UpdateFromSharingMember).createdTasks
        assertThat(createdTasks.size).isEqualTo(2)

        val expectedReservationTimeout = Instant.now().plus(taskConfigProperties.taskReservationTimeout)

        // reserve tasks
        val reservationResponse1 = reserveTasks(TaskStep.CleanAndSync)
        val reservedTasks = reservationResponse1.reservedTasks

        // expect the correct number of entries with the correct timeout
        assertThat(reservationResponse1.timeout).isCloseTo(expectedReservationTimeout, WITHIN_ALLOWED_TIME_OFFSET)
        assertThat(reservedTasks.size).isEqualTo(2)
        assertThat(reservedTasks.map { it.taskId }).isEqualTo(createdTasks.map { it.taskId })

        // ...and with the correct business partner information
        assertThat(reservedTasks[0].businessPartner.generic).isEqualTo(BusinessPartnerTestValues.businessPartner1)
        assertThat(reservedTasks[1].businessPartner.generic).isEqualTo(BusinessPartnerTestValues.businessPartner2)
        assertThat(reservedTasks[1].businessPartner.legalEntity).isNull()
        assertThat(reservedTasks[1].businessPartner.site).isNull()
        assertThat(reservedTasks[1].businessPartner.address).isNull()

        // trying to reserve more tasks returns no additional entries
        val reservationResponse2 = reserveTasks(TaskStep.CleanAndSync)
        assertThat(reservationResponse2.reservedTasks.size).isEqualTo(0)

        // check searchTaskStates response
        val statesResponse = searchTaskStates(reservedTasks.map { it.taskId })
        assertThat(statesResponse.tasks.size).isEqualTo(2)
        statesResponse.tasks.forEach { stateDto ->
            assertThat(stateDto.businessPartnerResult).isNull()
            val processingState = stateDto.processingState
            // stepState should have changed to Reserved
            assertProcessingStateDto(processingState, ResultState.Pending, TaskStep.CleanAndSync, StepState.Reserved)
            assertThat(processingState.errors).isEqualTo(emptyList<TaskErrorDto>())
            assertThat(processingState.modifiedAt).isAfter(processingState.createdAt)
            assertThat(processingState.modifiedAt).isCloseTo(Instant.now(), WITHIN_ALLOWED_TIME_OFFSET)
        }
    }

    /**
     * GIVEN some tasks were created in UpdateFromPool mode
     * WHEN reserving some tasks in step CleanAndSync
     *  THEN expect reservation returns no results
     */
    @Test
    fun `request reservation for wrong step`() {
        // create tasks
        createTasks(TaskMode.UpdateFromPool)

        // try reservation for wrong step
        val reservedTasks = reserveTasks(TaskStep.CleanAndSync).reservedTasks
        assertThat(reservedTasks.size).isEqualTo(0)
    }

    /**
     * GIVEN some tasks were created
     * WHEN reserving one task in step CleanAndSync
     *  THEN expect first task and state to switch to stepState==Reserved
     * WHEN resolving this task
     *  THEN expect state to switch to step==PoolSync and stepState==Queued
     * WHEN reserving this task in step PoolSync
     *  THEN expect state to switch to stepState==Reserved
     * WHEN resolving this task
     *  THEN expect state to switch to resultState==Success and stepState==Success and correct business partner data
     */
    @Test
    fun `post cleaning results for all steps`() {
        // create tasks
        createTasks()

        // reserve task for step==CleanAndSync
        val reservedTasks1 = reserveTasks(TaskStep.CleanAndSync, 1).reservedTasks
        val taskId = reservedTasks1.single().taskId
        assertThat(reservedTasks1[0].businessPartner.generic).isEqualTo(BusinessPartnerTestValues.businessPartner1)

        // now in stepState==Reserved
        assertProcessingStateDto(
            searchTaskStates(listOf(taskId)).tasks.single().processingState,
            ResultState.Pending, TaskStep.CleanAndSync, StepState.Reserved
        )

        // resolve task
        val businessPartnerFull1 = BusinessPartnerTestValues.businessPartner2Full
        val resultEntry1 = TaskStepResultEntryDto(
            taskId = taskId,
            businessPartner = businessPartnerFull1
        )
        resolveTasks(TaskStep.CleanAndSync, listOf(resultEntry1))

        // now in next step and stepState==Queued
        assertProcessingStateDto(
            searchTaskStates(listOf(taskId)).tasks.single().processingState,
            ResultState.Pending, TaskStep.PoolSync, StepState.Queued
        )

        // reserve task for step==PoolSync
        val reservedTasks2 = reserveTasks(TaskStep.PoolSync, 3).reservedTasks
        assertThat(reservedTasks2.size).isEqualTo(1)
        assertThat(reservedTasks2.single().businessPartner).isEqualTo(businessPartnerFull1)

        // now in stepState==Queued
        val stateDto = searchTaskStates(listOf(taskId)).tasks.single()
        assertProcessingStateDto(
            stateDto.processingState,
            ResultState.Pending, TaskStep.PoolSync, StepState.Reserved
        )
        assertThat(stateDto.businessPartnerResult).isNull()

        // resolve task again
        val businessPartnerFull2 = businessPartnerFull1.copy(
            generic = businessPartnerFull1.generic.copy(
                legalEntityBpn = "BPNL-test"
            )
        )
        val resultEntry2 = TaskStepResultEntryDto(
            taskId = taskId,
            businessPartner = businessPartnerFull2
        )
        resolveTasks(TaskStep.PoolSync, listOf(resultEntry2))

        // final step -> now in stepState==Success
        val finalStateDto = searchTaskStates(listOf(taskId)).tasks.single()
        assertProcessingStateDto(
            finalStateDto.processingState,
            ResultState.Success, TaskStep.PoolSync, StepState.Success
        )
        // check returned BP
        assertThat(finalStateDto.businessPartnerResult).isEqualTo(businessPartnerFull2.generic)
    }

    /**
     * GIVEN some tasks were created and reserved
     * WHEN resolving this task an error
     *  THEN expect state to switch to resultState==Error and stepState==Error
     * WHEN reserving this task in step PoolSync
     *  THEN expect state to switch to stepState==Reserved
     * WHEN resolving this task
     *  THEN expect state to switch to resultState==Success and stepState==Success and correct business partner data
     */
    @Test
    fun `post cleaning result with error`() {
        // create tasks
        createTasks()

        // reserve task for step==CleanAndSync
        val taskId = reserveTasks(TaskStep.CleanAndSync, 1).reservedTasks.single().taskId

        // resolve task with error
        val errorDto = TaskErrorDto(TaskErrorType.Unspecified, "Unfortunate event")
        val resultEntry = TaskStepResultEntryDto(
            taskId = taskId,
            errors = listOf(errorDto)
        )
        resolveTasks(TaskStep.CleanAndSync, listOf(resultEntry))

        // now in error state
        val stateDto = searchTaskStates(listOf(taskId)).tasks.single()
        assertProcessingStateDto(
            stateDto.processingState,
            ResultState.Error, TaskStep.CleanAndSync, StepState.Error
        )
        assertThat(stateDto.businessPartnerResult).isNull()
        // expect error in response
        assertThat(stateDto.processingState.errors.single()).isEqualTo(errorDto)
    }

    /**
     * WHEN requesting cleaning of too many business partners (over the upsert limit)
     * THEN throw exception
     */
    @Test
    fun `expect exception on requesting too many cleaning tasks`() {
        // Create entries above the upsert limit of 3
        val businessPartners = listOf(
            BusinessPartnerTestValues.businessPartner1,
            BusinessPartnerTestValues.businessPartner1,
            BusinessPartnerTestValues.businessPartner1,
            BusinessPartnerTestValues.businessPartner1
        )

        assertBadRequestException {
            createTasks(businessPartners = businessPartners)
        }
    }

    /**
     * WHEN reserving too many cleaning tasks (over the upsert limit)
     * THEN throw exception
     */
    @Test
    fun `expect exception on requesting too many reservations`() {
        // Create entries above the upsert limit of 3
        assertBadRequestException {
            reserveTasks(TaskStep.CleanAndSync, 200)
        }
    }

    /**
     * WHEN posting too many cleaning results (over the upsert limit)
     * THEN throw exception
     */
    @Test
    fun `expect exception on posting too many task results`() {
        val validResultEntry = TaskStepResultEntryDto(
            taskId = "0",
            businessPartner = null,
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
            resolveTasks(TaskStep.CleanAndSync, resultEntries)
        }
    }

    /**
     * GIVEN some resolved tasks
     * WHEN trying to resolve a task with different task id
     *  THEN expect a BAD_REQUEST
     * WHEN trying to resolve a task with empty content
     *  THEN expect a BAD_REQUEST
     * WHEN trying to resolve a task twice
     *  THEN expect a BAD_REQUEST
     */
    @Test
    fun `expect exceptions on posting inconsistent task results`() {
        // create tasks
        createTasks()

        // reserve tasks
        val tasksIds = reserveTasks(TaskStep.CleanAndSync).reservedTasks.map { it.taskId }
        assertThat(tasksIds.size).isEqualTo(2)

        // post wrong task ids
        assertBadRequestException {
            resolveTasks(
                TaskStep.CleanAndSync,
                listOf(
                    TaskStepResultEntryDto(
                        taskId = "WRONG-ID"
                    )
                )
            )
        }

        // post correct task id but empty content
        assertBadRequestException {
            resolveTasks(
                TaskStep.CleanAndSync,
                listOf(
                    TaskStepResultEntryDto(
                        taskId = tasksIds[0]
                    )
                )
            )
        }

        // post correct task id but wrong step (Clean)
        assertBadRequestException {
            resolveTasks(
                TaskStep.Clean,
                listOf(
                    TaskStepResultEntryDto(
                        taskId = tasksIds[0],
                        businessPartner = BusinessPartnerTestValues.businessPartner1Full
                    )
                )
            )
        }

        // post correct task id with business partner content
        resolveTasks(
            TaskStep.CleanAndSync,
            listOf(
                TaskStepResultEntryDto(
                    taskId = tasksIds[0],
                    businessPartner = BusinessPartnerTestValues.businessPartner1Full
                )
            )
        )

        // post task twice
        assertBadRequestException {
            resolveTasks(
                TaskStep.CleanAndSync,
                listOf(
                    TaskStepResultEntryDto(
                        taskId = tasksIds[0],
                        businessPartner = BusinessPartnerTestValues.businessPartner1Full
                    )
                )
            )
        }


        // post correct task id with error content
        resolveTasks(
            TaskStep.CleanAndSync,
            listOf(
                TaskStepResultEntryDto(
                    tasksIds[1], errors = listOf(
                        TaskErrorDto(type = TaskErrorType.Unspecified, "ERROR")
                    )
                )
            )
        )
    }

    private fun createTasks(mode: TaskMode = TaskMode.UpdateFromSharingMember, businessPartners: List<BusinessPartnerGenericDto>? = null): TaskCreateResponse =
        orchestratorClient.goldenRecordTasks.createTasks(
            TaskCreateRequest(
                mode = mode,
                businessPartners = businessPartners ?: listOf(BusinessPartnerTestValues.businessPartner1, BusinessPartnerTestValues.businessPartner2)
            )
        )

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

    private fun searchTaskStates(taskIds: List<String>) =
        orchestratorClient.goldenRecordTasks.searchTaskStates(
            TaskStateRequest(taskIds)
        )

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
}
