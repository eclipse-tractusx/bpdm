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
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.StepStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateV7Request
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.reactive.function.client.WebClientResponseException

class RelationTaskCreationV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * WHEN gate creates a new relation task for new sharing member record
     * THEN gate receives created task response with new sharing member record ID
     */
    @ParameterizedTest
    @EnumSource(TaskModeV7::class)
    fun `create relation task for new sharing member record`(taskMode: TaskModeV7){
        //WHEN
        val requestEntry = requestFactory.buildRelationTaskCreateEntry(testName, recordId = null)
        val createRequest = TaskCreateRelationsV7Request(taskMode, listOf(requestEntry))
        val response = orchestratorClient.relationsGoldenRecordTasks.createTasks(createRequest)

        //THEN
        val expectedEntry = resultFactory.buildCreatedRelationTaskClientState(requestEntry.businessPartnerRelations, taskMode)
        val expectedResponse = TaskCreateRelationsV7Response(listOf(expectedEntry))
        assertRepo.assertRelationTaskCreateResponseEqual(response, expectedResponse, isForNewRecord = true)
    }

    /**
     * GIVEN sharing member record
     * WHEN user requests new relation task for new data for sharing member record
     * THEN user sees created task in step first step for sharing member record
     */
    @ParameterizedTest
    @EnumSource(TaskModeV7::class)
    fun `create new relation task for existing sharing member record`(taskMode: TaskModeV7){
        //GIVEN
        val recordId = testDataClient.createRelationTask("$testName 1").recordId

        //WHEN
        val newTask = requestFactory.buildRelationTaskCreateEntry("$testName 2", recordId)
        val createRequest = TaskCreateRelationsV7Request(taskMode, listOf(newTask))
        val createResult = orchestratorClient.relationsGoldenRecordTasks.createTasks(createRequest)

        //THEN
        val expectedResult = TaskCreateRelationsV7Response(listOf(resultFactory.buildCreatedRelationTaskClientState(
            relation = newTask.businessPartnerRelations,
            taskMode = taskMode,
            recordId = recordId
        )))
        assertRepo.assertRelationTaskCreateResponseEqual(createResult, expectedResult, isForNewRecord = false)
    }

    /**
     * WHEN user requests new relation task for non-existing sharing member record
     * THEN user sees 400 BAD REQUEST error
     */
    @ParameterizedTest
    @EnumSource(TaskModeV7::class)
    fun `try create relation task for not existing sharing member record`(taskMode: TaskModeV7){
        //WHEN
        val newTask = requestFactory.buildRelationTaskCreateEntry(testName, "NOT EXISTING")
        val requestBody = TaskCreateRelationsV7Request(taskMode, listOf(newTask))
        val createRequest: () -> Unit
                =  { orchestratorClient.relationsGoldenRecordTasks.createTasks(requestBody) }

        //THEN
        Assertions.assertThatThrownBy(createRequest).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN existing relation task in pending
     * WHEN user creates a new task for the same sharing member record
     * THEN user sees existing task aborted
     */
    @ParameterizedTest
    @EnumSource(TaskModeV7::class)
    fun `abort task when creating new one`(taskMode: TaskModeV7){
        //GIVEN
        val createdTaskOld = testDataClient.createRelationTask(testName)

        //WHEN
        val requestEntry = requestFactory.buildRelationTaskCreateEntry(testName, createdTaskOld.recordId)
        val createRequest = TaskCreateRelationsV7Request(taskMode, listOf(requestEntry))
        orchestratorClient.relationsGoldenRecordTasks.createTasks(createRequest)

        //THEN
        val oldTaskEntry = TaskStateV7Request.Entry(createdTaskOld.taskId, createdTaskOld.recordId)
        val actualOldTaskResponse = orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(TaskStateV7Request(listOf(oldTaskEntry)))

        val expectedEntry = createdTaskOld.copy(
            businessPartnerRelationsResult = createdTaskOld.businessPartnerRelationsResult,
            processingState = createdTaskOld.processingState.copy(
                resultState = ResultStateV7.Error,
                step = createdTaskOld.processingState.step,
                stepState = StepStateV7.Error,
                errors = emptyList()
            )
        )
        val expectedResponse = TaskRelationsStateV7Response(listOf(expectedEntry))

        assertRepo.assertRelationTaskStateResponseEqual(actualOldTaskResponse, expectedResponse)
    }

    /**
     * WHEN trying creating too many relation tasks (over the upsert limit)
     * THEN throw 400 BAD REQUEST error
     */
    @ParameterizedTest
    @EnumSource(TaskModeV7::class)
    fun `try create too many relation tasks`(taskMode: TaskModeV7) {
        //WHEN
        val requestBody = TaskCreateRelationsV7Request(
            taskMode,
            (1 .. 11).map { requestFactory.buildRelationTaskCreateEntry("$testName $it", null) }
        )
        val request: () -> Unit = { orchestratorClient.relationsGoldenRecordTasks.createTasks(requestBody) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }
}