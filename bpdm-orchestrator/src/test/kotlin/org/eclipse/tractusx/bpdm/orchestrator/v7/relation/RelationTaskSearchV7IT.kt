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

package org.eclipse.tractusx.bpdm.orchestrator.v7.relation

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.orchestrator.v7.UnscheduledOrchestratorTestBaseV7
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class RelationTaskSearchV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * GIVEN created relation task
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search created relation task`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedResponse = TaskRelationsStateResponse(listOf(createdTask))

        assertRepo.assertRelationTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN relation task has been reserved
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search reserved relation task`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        testDataClient.reserveRelationTask(createdTask)

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(processingState = createdTask.processingState.copy(stepState = StepState.Reserved))
        val expectedResponse = TaskRelationsStateResponse(listOf(expectedEntry))

        assertRepo.assertRelationTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN relation task has been resolved
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search resolved relation task`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        val resultRequest = testDataClient.reserveAndResolveRelationTask(createdTask, "Resolved $testName")

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(
            businessPartnerRelationsResult = resultRequest.businessPartnerRelations,
            processingState = createdTask.processingState.copy(step = TaskStep.PoolSync)
        )
        val expectedResponse = TaskRelationsStateResponse(listOf(expectedEntry))

        assertRepo.assertRelationTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN relation task is successfully finished
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search successful relation task`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        testDataClient.reserveAndResolveRelationTask(createdTask.taskId, TaskStep.CleanAndSync, "Resolved $testName")
        val successRequest = testDataClient.reserveAndResolveRelationTask(createdTask.taskId, TaskStep.PoolSync, "Success $testName")

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(
            businessPartnerRelationsResult = successRequest.businessPartnerRelations,
            processingState = createdTask.processingState.copy(
                resultState = ResultState.Success,
                step = TaskStep.PoolSync,
                stepState = StepState.Success
            )
        )
        val expectedResponse = TaskRelationsStateResponse(listOf(expectedEntry))

        assertRepo.assertRelationTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN relation task is failed
     * WHEN user searches for task
     * THEN user finds task
     */
    @Test
    fun `search failed relation task`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        val failRequest = testDataClient.failRelationTask(createdTask)

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, createdTask.recordId)))
        val searchResponse = orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedEntry = createdTask.copy(
            businessPartnerRelationsResult = createdTask.businessPartnerRelationsResult,
            processingState = createdTask.processingState.copy(
                resultState = ResultState.Error,
                step = createdTask.processingState.step,
                stepState = StepState.Error,
                errors = failRequest.errors
            )
        )
        val expectedResponse = TaskRelationsStateResponse(listOf(expectedEntry))

        assertRepo.assertRelationTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * WHEN user searches for not existing relation task id
     * THEN user sees HTTP BAD REQUEST response
     */
    @Test
    fun `try search not existing relation task id`(){
        //WHEN
        val searchRequestBody = TaskStateRequest(listOf(TaskStateRequest.Entry("NOT EXISTING", "NOT EXISTING")))
        val searchRequest : () -> Unit = { orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(searchRequestBody) }

        //THEN
        Assertions.assertThatThrownBy(searchRequest).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN relation task with task id
     * WHEN user searches for wrong sharing member record id
     * THEN user sees empty response
     */
    @Test
    fun `search not existing relation sharing member record`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask.taskId, "NOT EXISTING")))
        val searchResponse = orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedResponse = TaskRelationsStateResponse(emptyList())

        assertRepo.assertRelationTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN task 1 and task 2
     * WHEN user searches for task by task id 1 and record id 2
     * THEN user sees empty response
     */
    @Test
    fun `search not matching relation task and record id`(){
        //GIVEN
        val createdTask1 = testDataClient.createRelationTask("$testName 1")
        val createdTask2 = testDataClient.createRelationTask("$testName 2")

        //WHEN
        val searchRequest = TaskStateRequest(listOf(TaskStateRequest.Entry(createdTask1.taskId, createdTask2.recordId)))
        val searchResponse = orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(searchRequest)

        //THEN
        val expectedResponse = TaskRelationsStateResponse(emptyList())

        assertRepo.assertRelationTaskStateResponseEqual(searchResponse, expectedResponse)
    }

    /**
     * GIVEN aborted relation task
     * WHEN user searches for task
     * THEN user finds task as aborted
     */
    @Test
    fun `search aborted relation task`(){
        //GIVEN
        val abortedTask = testDataClient.createRelationTask(testName)
        testDataClient.createRelationTask(testName, recordId = abortedTask.recordId)

        //WHEN
        val searchEntry = TaskStateRequest.Entry(abortedTask.taskId, abortedTask.recordId)
        val searchResponse = orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(TaskStateRequest(listOf(searchEntry)))

        //THEN
        val expectedResponse = TaskRelationsStateResponse(listOf(abortedTask.copy(
            processingState = abortedTask.processingState.copy(
                resultState = ResultState.Error,
                stepState = StepState.Error
            )
        )))

        assertRepo.assertRelationTaskStateResponseEqual(searchResponse, expectedResponse)
    }

}