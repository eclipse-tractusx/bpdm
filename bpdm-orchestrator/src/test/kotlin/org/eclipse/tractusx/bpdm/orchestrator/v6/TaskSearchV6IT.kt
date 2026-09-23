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

package org.eclipse.tractusx.bpdm.orchestrator.v6

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.orchestrator.api.v6.model.ResultStateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.StepStateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStateRequestV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStateResponseV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepV6
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class TaskSearchV6IT: UnscheduledOrchestratorTestBaseV6() {

    /**
     * GIVEN created task
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search created task`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName)

        //WHEN
        val searchRequest = TaskStateRequestV6(listOf(TaskStateRequestV6.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedResponse = TaskStateResponseV6(listOf(createdTask))

        assertRepository.assertTaskStateResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task has been reserved
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search reserved task`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName)
        testDataClient.reserveTasks(createdTask.processingState.step).reservedTasks.single()

        //WHEN
        val searchRequest = TaskStateRequestV6(listOf(TaskStateRequestV6.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(processingState = createdTask.processingState.copy(stepState = StepStateV6.Reserved))
        val expectedResponse = TaskStateResponseV6(listOf(expectedEntry))

        assertRepository.assertTaskStateResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task has been resolved
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search resolved task`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName)
        val resultRequest = testDataClient.resolveTask(createdTask.taskId, createdTask.processingState.step, "Resolved $testName")

        //WHEN
        val searchRequest = TaskStateRequestV6(listOf(TaskStateRequestV6.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(businessPartnerResult = resultRequest.businessPartner, processingState = createdTask.processingState.copy(step = TaskStepV6.PoolSync))
        val expectedResponse = TaskStateResponseV6(listOf(expectedEntry))

        assertRepository.assertTaskStateResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task is successfully finished
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search successful task`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName)
        testDataClient.resolveTask(createdTask.taskId, TaskStepV6.CleanAndSync, "Resolved $testName")
        val successRequest = testDataClient.resolveTask(createdTask.taskId, TaskStepV6.PoolSync, "Success $testName")

        //WHEN
        val searchRequest = TaskStateRequestV6(listOf(TaskStateRequestV6.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(
            businessPartnerResult = successRequest.businessPartner,
            processingState = createdTask.processingState.copy(
                resultState = ResultStateV6.Success,
                step = TaskStepV6.PoolSync,
                stepState = StepStateV6.Success
            )
        )
        val expectedResponse = TaskStateResponseV6(listOf(expectedEntry))

        assertRepository.assertTaskStateResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task is failed
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search failed task`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName)
        val failRequest = testDataClient.failTask(createdTask.taskId, createdTask.processingState.step)

        //WHEN
        val searchRequest = TaskStateRequestV6(listOf(TaskStateRequestV6.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(
            businessPartnerResult = createdTask.businessPartnerResult,
            processingState = createdTask.processingState.copy(
                resultState = ResultStateV6.Error,
                step = createdTask.processingState.step,
                stepState = StepStateV6.Error,
                errors = failRequest.errors
            )
        )
        val expectedResponse = TaskStateResponseV6(listOf(expectedEntry))

        assertRepository.assertTaskStateResponse(searchResponse, expectedResponse)
    }

    /**
     * WHEN user searches for not existing task id
     * THEN user sees HTTP BAD REQUEST response
     */
    @Test
    fun `try search not existing task id`(){
        //WHEN
        val searchRequestBody = TaskStateRequestV6(listOf(TaskStateRequestV6.Entry("NOT EXISTING", "NOT EXISTING")))
        val searchRequest : () -> Unit = { orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequestBody) }

        //THEN
        Assertions.assertThatThrownBy(searchRequest).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN task with task id
     * WHEN user searches for wrong sharing member record id
     * THEN user sees empty response
     */
    @Test
    fun `search not existing sharing member record`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName)

        //WHEN
        val searchRequest = TaskStateRequestV6(listOf(TaskStateRequestV6.Entry(createdTask.taskId, "NOT EXISTING")))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedResponse = TaskStateResponseV6(emptyList())

        assertRepository.assertTaskStateResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task 1 and task 2
     * WHEN user searches for task by task id 1 and record id 2
     * THEN user sees empty response
     */
    @Test
    fun `search not matching task and record id`(){
        //GIVEN
        val createdTask1 = testDataClient.createTask("$testName 1")
        val createdTask2 = testDataClient.createTask("$testName 2")

        //WHEN
        val searchRequest = TaskStateRequestV6(listOf(TaskStateRequestV6.Entry(createdTask1.taskId, createdTask2.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedResponse = TaskStateResponseV6(emptyList())

        assertRepository.assertTaskStateResponse(searchResponse, expectedResponse)
    }
}