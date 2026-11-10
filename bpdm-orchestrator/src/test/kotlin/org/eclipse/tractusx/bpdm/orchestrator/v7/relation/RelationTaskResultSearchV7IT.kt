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
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Response
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class RelationTaskResultSearchV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * GIVEN relation task in pending state
     * WHEN user searches for task ID
     * THEN user sees task in pending state result
     */
    @Test
    fun `search pending relation task`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)

        //WHEN
        val searchRequest = TaskResultStateSearchV7Request(listOf(createdTask.taskId))
        val searchResult = orchestratorClient.relationsGoldenRecordTasks.searchTaskResultStates(searchRequest)

        //THEN
        val expectedResult = TaskResultStateSearchV7Response(listOf(ResultStateV7.Pending))
        assertRepo.assertTaskResultStateSearchResponseEqual(searchResult, expectedResult)
    }

    /**
     * GIVEN failed relation task
     * WHEN user searches for task ID
     * THEN user sees task in error state result
     */
    @Test
    fun `search error relation task`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        testDataClient.failRelationTask(createdTask)

        //WHEN
        val searchRequest = TaskResultStateSearchV7Request(listOf(createdTask.taskId))
        val searchResult = orchestratorClient.relationsGoldenRecordTasks.searchTaskResultStates(searchRequest)

        //THEN
        val expectedResult = TaskResultStateSearchV7Response(listOf(ResultStateV7.Error))
        assertRepo.assertTaskResultStateSearchResponseEqual(searchResult, expectedResult)
    }

    /**
     * GIVEN successfully completed relation task
     * WHEN user searches for task ID
     * THEN user sees task in success state result
     */
    @Test
    fun `search success relation task`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName, TaskModeV7.UpdateFromPool)
        testDataClient.reserveAndResolveRelationTask(createdTask, "$testName Cleaned")

        //WHEN
        val searchRequest = TaskResultStateSearchV7Request(listOf(createdTask.taskId))
        val searchResult = orchestratorClient.relationsGoldenRecordTasks.searchTaskResultStates(searchRequest)

        //THEN
        val expectedResult = TaskResultStateSearchV7Response(listOf(ResultStateV7.Success))
        assertRepo.assertTaskResultStateSearchResponseEqual(searchResult, expectedResult)
    }

    /**
     * GIVEN relation tasks
     * WHEN user searches for non-existing task ID
     * THEN user sees 400 BAD REQUEST error
     */
    @Test
    fun `search no existing relation task id`(){
        //GIVEN
        testDataClient.createRelationTask(testName)

        //WHEN
        val searchRequestBody = TaskResultStateSearchV7Request(listOf("NOT EXISTS"))
        val searchRequest: () -> Unit = { orchestratorClient.relationsGoldenRecordTasks.searchTaskResultStates(searchRequestBody) }

        //THEN
        Assertions.assertThatThrownBy(searchRequest).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }
}