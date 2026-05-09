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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class BusinessPartnerTaskResultSearchV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * GIVEN business partner task in pending state
     * WHEN user searches for task ID
     * THEN user sees task in pending state result
     */
    @Test
    fun `search pending business partner task`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)

        //WHEN
        val searchRequest = TaskResultStateSearchRequest(listOf(createdTask.taskId))
        val searchResult = orchestratorClient.goldenRecordTasks.searchTaskResultStates(searchRequest)

        //THEN
        val expectedResult = TaskResultStateSearchResponse(listOf(ResultState.Pending))
        assertRepo.assertTaskResultStateSearchResponseEqual(searchResult, expectedResult)
    }

    /**
     * GIVEN failed business partner task
     * WHEN user searches for task ID
     * THEN user sees task in error state result
     */
    @Test
    fun `search error business partner task`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        testDataClient.failBusinessPartnerTask(createdTask.taskId, TaskStep.CleanAndSync)

        //WHEN
        val searchRequest = TaskResultStateSearchRequest(listOf(createdTask.taskId))
        val searchResult = orchestratorClient.goldenRecordTasks.searchTaskResultStates(searchRequest)

        //THEN
        val expectedResult = TaskResultStateSearchResponse(listOf(ResultState.Error))
        assertRepo.assertTaskResultStateSearchResponseEqual(searchResult, expectedResult)
    }

    /**
     * GIVEN successfully completed business partner task
     * WHEN user searches for task ID
     * THEN user sees task in success state result
     */
    @Test
    fun `search success business partner task`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName, TaskMode.UpdateFromPool)
        testDataClient.reserveAndResolveBusinessPartnerTask(createdTask.taskId, TaskStep.Clean, "$testName Cleaned")

        //WHEN
        val searchRequest = TaskResultStateSearchRequest(listOf(createdTask.taskId))
        val searchResult = orchestratorClient.goldenRecordTasks.searchTaskResultStates(searchRequest)

        //THEN
        val expectedResult = TaskResultStateSearchResponse(listOf(ResultState.Success))
        assertRepo.assertTaskResultStateSearchResponseEqual(searchResult, expectedResult)
    }

    /**
     * GIVEN business partner tasks
     * WHEN user searches for non-existing task ID
     * THEN user sees 400 BAD REQUEST error
     */
    @Test
    @Disabled("ToDo: https://github.com/eclipse-tractusx/bpdm/issues/1597")
    fun `search no existing task id`(){
        //GIVEN
        testDataClient.createBusinessPartnerTask(testName)

        //WHEN
        val searchRequestBody = TaskResultStateSearchRequest(listOf("NOT EXISTS"))
        val searchRequest: () -> Unit = { orchestratorClient.goldenRecordTasks.searchTaskResultStates(searchRequestBody) }

        //THEN
        Assertions.assertThatThrownBy(searchRequest).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }
}