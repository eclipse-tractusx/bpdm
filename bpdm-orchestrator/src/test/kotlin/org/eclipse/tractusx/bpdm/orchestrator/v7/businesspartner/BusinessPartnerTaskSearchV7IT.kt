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

package org.eclipse.tractusx.bpdm.orchestrator.v7.businesspartner

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.orchestrator.v7.UnscheduledOrchestratorTestBaseV7
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class BusinessPartnerTaskSearchV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * GIVEN created task
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search created task`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedResponse = TaskStateResponse(listOf(createdTask))

        assertRepo.assertBusinessPartnerTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task has been reserved
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search reserved task`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        testDataClient.reserveBusinessPartnerTasks(createdTask.processingState.step)

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(processingState = createdTask.processingState.copy(stepState = StepState.Reserved))
        val expectedResponse = TaskStateResponse(listOf(expectedEntry))

        assertRepo.assertBusinessPartnerTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task has been resolved
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search resolved task`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        val resultRequest = testDataClient.reserveAndResolveBusinessPartnerTask(createdTask.taskId, createdTask.processingState.step, "Resolved $testName")

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(businessPartnerResult = resultRequest.businessPartner, processingState = createdTask.processingState.copy(step = TaskStep.PoolSync))
        val expectedResponse = TaskStateResponse(listOf(expectedEntry))

        assertRepo.assertBusinessPartnerTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task is successfully finished
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search successful task`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        testDataClient.reserveAndResolveBusinessPartnerTask(createdTask.taskId, TaskStep.CleanAndSync, "Resolved $testName")
        val successRequest = testDataClient.reserveAndResolveBusinessPartnerTask(createdTask.taskId, TaskStep.PoolSync, "Success $testName")

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(
            businessPartnerResult = successRequest.businessPartner,
            processingState = createdTask.processingState.copy(
                resultState = ResultState.Success,
                step = TaskStep.PoolSync,
                stepState = StepState.Success
            )
        )
        val expectedResponse = TaskStateResponse(listOf(expectedEntry))

        assertRepo.assertBusinessPartnerTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task is failed
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search failed task`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        val failRequest = testDataClient.failBusinessPartnerTask(createdTask.taskId, createdTask.processingState.step)

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(
            businessPartnerResult = createdTask.businessPartnerResult,
            processingState = createdTask.processingState.copy(
                resultState = ResultState.Error,
                step = createdTask.processingState.step,
                stepState = StepState.Error,
                errors = failRequest.errors
            )
        )
        val expectedResponse = TaskStateResponse(listOf(expectedEntry))

        assertRepo.assertBusinessPartnerTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * WHEN user searches for not existing task id
     * THEN user sees HTTP BAD REQUEST response
     */
    @Test
    fun `try search not existing task id`(){
        //WHEN
        val searchRequestBody = TaskStateRequest(listOf(TaskStateRequest.Entry("NOT EXISTING", "NOT EXISTING")))
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
        val createdTask = testDataClient.createBusinessPartnerTask(testName)

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, "NOT EXISTING")))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedResponse = TaskStateResponse(emptyList())

        assertRepo.assertBusinessPartnerTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task 1 and task 2
     * WHEN user searches for task by task id 1 and record id 2
     * THEN user sees empty response
     */
    @Test
    fun `search not matching task and record id`(){
        //GIVEN
        val createdTask1 = testDataClient.createBusinessPartnerTask("$testName 1")
        val createdTask2 = testDataClient.createBusinessPartnerTask("$testName 2")

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask1.taskId, createdTask2.recordId)))
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedResponse = TaskStateResponse(emptyList())

        assertRepo.assertBusinessPartnerTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN aborted business partner task
     * WHEN user searches for task
     * THEN user finds task as aborted
     */
    @Test
    fun `search aborted business partner task`(){
        //GIVEN
        val abortedTask = testDataClient.createBusinessPartnerTask(testName)
        testDataClient.createBusinessPartnerTask(testName, recordId = abortedTask.recordId)

        //WHEN
        val searchEntry = TaskStateRequest.Entry(abortedTask.taskId, abortedTask.recordId)
        val searchResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(TaskStateRequest(listOf(searchEntry)))

        //THEN
        val expectedResponse = TaskStateResponse(listOf(abortedTask.copy(
            processingState = abortedTask.processingState.copy(
                resultState = ResultState.Error,
                stepState = StepState.Error
            )
        )))

        assertRepo.assertBusinessPartnerTaskStateResponseEqual(searchResponse, expectedResponse)
    }
}