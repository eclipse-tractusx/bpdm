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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.reactive.function.client.WebClientResponseException

class BusinessPartnerTaskCreationV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * WHEN gate creates a new task for new sharing member record
     * THEN gate receives created task response with new sharing member record ID
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `create task for new sharing member record`(taskMode: TaskMode){
        //WHEN
        val requestEntry = requestFactory.buildBusinessPartnerTaskCreateEntry(testName).copy(recordId = null)
        val createRequest = TaskCreateRequest(taskMode, listOf(requestEntry))
        val response = orchestratorClient.goldenRecordTasks.createTasks(createRequest)

        //THEN
        val expectedEntry = resultFactory.buildCreatedBusinessPartnerTaskClientState(requestEntry.businessPartner, taskMode)
        val expectedResponse = TaskCreateResponse(listOf(expectedEntry))
        assertRepo.assertBusinessPartnerTaskCreateResponseEqual(response, expectedResponse, isForNewRecord = true)
    }

    /**
     * GIVEN sharing member record
     * WHEN user requests new task for new data for sharing member record
     * THEN user sees created task in step first step for sharing member record
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `create new sharing member update task for existing sharing member record`(taskMode: TaskMode){
        //GIVEN
        val recordId = testDataClient.createBusinessPartnerTask("$testName 1").recordId

        //WHEN
        val newTask = requestFactory.buildBusinessPartnerTaskCreateEntry("$testName 2").copy(recordId = recordId)
        val createRequest = TaskCreateRequest(taskMode, listOf(newTask))
        val createResult = orchestratorClient.goldenRecordTasks.createTasks(createRequest)

        //THEN
        val expectedResult = TaskCreateResponse(listOf(resultFactory.buildCreatedBusinessPartnerTaskClientState(
            businessPartner = createRequest.requests.single().businessPartner,
            taskMode = taskMode,
            recordId = recordId
        )))
        assertRepo.assertBusinessPartnerTaskCreateResponseEqual(createResult, expectedResult, isForNewRecord = false)
    }

    /**
     * WHEN user requests new task for non-existing sharing member record
     * THEN user sees 400 BAD REQUEST error
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `try create task for not existing sharing member record`(taskMode: TaskMode){
        //WHEN
        val newTask = requestFactory.buildBusinessPartnerTaskCreateEntry(testName).copy(recordId = "NOT EXISTING")
        val requestBody = TaskCreateRequest(taskMode, listOf(newTask))
        val createRequest: () -> Unit
                =  { orchestratorClient.goldenRecordTasks.createTasks(requestBody) }

        //THEN
        Assertions.assertThatThrownBy(createRequest).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN existing task in pending
     * WHEN user creates a new task for the same sharing member record
     * THEN user sees existing task aborted
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `abort task when creating new one`(taskMode: TaskMode){
        //GIVEN
        val createdTaskOld = testDataClient.createBusinessPartnerTask(testName)

        //WHEN
        val requestEntry = requestFactory.buildBusinessPartnerTaskCreateEntry(testName).copy(recordId = createdTaskOld.recordId)
        val createRequest = TaskCreateRequest(taskMode, listOf(requestEntry))
        orchestratorClient.goldenRecordTasks.createTasks(createRequest)

        //THEN
        val oldTaskEntry = TaskStateRequest.Entry(createdTaskOld.taskId, createdTaskOld.recordId)
        val actualOldTaskResponse = orchestratorClient.goldenRecordTasks.searchTaskStates(TaskStateRequest(listOf(oldTaskEntry)))

        val expectedEntry = createdTaskOld.copy(
            businessPartnerResult = createdTaskOld.businessPartnerResult,
            processingState = createdTaskOld.processingState.copy(
                resultState = ResultState.Error,
                step = createdTaskOld.processingState.step,
                stepState = StepState.Error,
                errors = emptyList()
            )
        )
        val expectedResponse = TaskStateResponse(listOf(expectedEntry))

        assertRepo.assertBusinessPartnerTaskStateResponseEqual(actualOldTaskResponse, expectedResponse)
    }

    /**
     * WHEN trying creating too many business partner tasks (over the upsert limit)
     * THEN throw 400 BAD REQUEST error
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `try create too many business partner tasks`(taskMode: TaskMode) {
        //WHEN
        val requestBody = TaskCreateRequest(
            taskMode,
            (1 .. 11).map { requestFactory.buildBusinessPartnerTaskCreateEntry("$testName $it") }
        )
        val request: () -> Unit = { orchestratorClient.goldenRecordTasks.createTasks(requestBody) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

}