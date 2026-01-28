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
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateResponse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.reactive.function.client.WebClientResponseException

class TaskCreationV6IT: UnscheduledOrchestratorTestV6() {

    /**
     * WHEN user requests new task for new data for new sharing member record
     * THEN user sees created task in first step for new sharing member record
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `create new sharing member update task for new sharing member record`(taskMode: TaskMode){
        //WHEN
        val newTask = requestFactory.buildTaskCreate(testName).copy(recordId = null)
        val createRequest = TaskCreateRequest(taskMode, listOf(newTask))
        val createResult = orchestratorClient.goldenRecordTasks.createTasks(createRequest)

        //THEN
        val expectedResult = TaskCreateResponse(listOf(expectedResultFactory.buildCreatedTaskClientState(createRequest.requests.single().businessPartner, taskMode)))
        assertRepository.assertCreatedTasksForNewSharingMemberRecords(createResult, expectedResult)
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
        val recordId = testDataClient.createSharingMemberRecord(testName)

        //WHEN
        val newTask = requestFactory.buildTaskCreate(testName).copy(recordId = recordId)
        val createRequest = TaskCreateRequest(taskMode, listOf(newTask))
        val createResult = orchestratorClient.goldenRecordTasks.createTasks(createRequest)

        //THEN
        val expectedResult = TaskCreateResponse(listOf(expectedResultFactory.buildCreatedTaskClientState(
            businessPartner = createRequest.requests.single().businessPartner,
            taskMode = taskMode,
            recordId = recordId
        )))
        assertRepository.assertCreatedTasksForExistingSharingMemberRecords(createResult, expectedResult)
    }

    /**
     * WHEN user requests new task for non-existing sharing member record
     * THEN user sees 400 BAD REQUEST error
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `try create task for not existing sharing member record`(taskMode: TaskMode){
        //WHEN
        val newTask = requestFactory.buildTaskCreate(testName).copy(recordId = "NOT EXISTING")
        val requestBody = TaskCreateRequest(taskMode, listOf(newTask))
        val createRequest: () -> Unit
                =  { orchestratorClient.goldenRecordTasks.createTasks(requestBody) }

        //THEN
        Assertions.assertThatThrownBy(createRequest).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }
}